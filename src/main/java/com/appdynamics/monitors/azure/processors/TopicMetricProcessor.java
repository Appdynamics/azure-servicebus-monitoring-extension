package com.appdynamics.monitors.azure.processors;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.azure.ExcludePatternPredicate;
import com.appdynamics.monitors.azure.IncludePatternPredicate;
import com.azure.core.http.rest.PagedIterable;
import com.azure.monitor.query.MetricsQueryAsyncClient;
import com.azure.monitor.query.models.*;
import com.azure.resourcemanager.servicebus.fluent.ServiceBusManagementClient;
import com.azure.resourcemanager.servicebus.fluent.models.SBTopicInner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Phaser;
import java.util.stream.Collectors;

import static com.appdynamics.monitors.azure.Constants.SEPARATOR;

public class TopicMetricProcessor implements Runnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(TopicMetricProcessor.class);

    private ServiceBusManagementClient serviceBusManagementClient;

    private MetricsQueryAsyncClient metricsQueryAsyncClient;
    private MetricWriteHelper metricWriteHelper;
    private String metricPrefix;
    private String displayName;

    private String resourceGroup;
    private String namespace;

    private List<String> includeTopics;
    private List<String> excludeTopics;
    private List<Map> topicMetricsFromConfig;
    private Phaser phaser;

    public TopicMetricProcessor(ServiceBusManagementClient serviceBusManagementClient, MetricsQueryAsyncClient metricsQueryAsyncClient, MetricWriteHelper metricWriteHelper, String metricPrefix, String displayName, String resourceGroup, String namespace, List<String> includeTopics, List<String> excludeTopics, List<Map> topicMetricsFromConfig, Phaser phaser) {
        this.serviceBusManagementClient = serviceBusManagementClient;
        this.metricsQueryAsyncClient = metricsQueryAsyncClient;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix = metricPrefix;
        this.displayName = displayName;
        this.resourceGroup = resourceGroup;
        this.namespace = namespace;
        this.includeTopics = includeTopics;
        this.excludeTopics = excludeTopics;
        this.topicMetricsFromConfig = topicMetricsFromConfig;
        this.phaser = phaser;
        this.phaser.register();
    }

    @Override
    public void run() {

        List<Metric> topicMetricList = Lists.newArrayList();

        try {

            PagedIterable<SBTopicInner> topicIterator = serviceBusManagementClient.getTopics().listByNamespace(resourceGroup, namespace);

            Map<String, String> topicsWithSBId = Flux.fromIterable(topicIterator)
                    .collectMap(topicInner -> topicInner.name(),
                            topicInner -> topicInner.id()).block();

            if(topicsWithSBId == null || topicsWithSBId.size() <= 0) {
                logger.info("Could not find topics in resource group [{}] namespace [{}]", resourceGroup, namespace);
                logger.info("Phaser arrived for Topic metric processor for server {}", displayName);
                phaser.arriveAndDeregister();
                return;
            }

            Set<String> allTopicNames = topicsWithSBId.keySet();
            String serviceBusResourceId = getServiceBusResourceId(topicsWithSBId);

            Set<String> filteredTopics;
            if ((includeTopics == null || includeTopics.isEmpty()) && (excludeTopics == null || excludeTopics.isEmpty())) {
                filteredTopics = allTopicNames;
            } else if (includeTopics != null && !includeTopics.isEmpty()) {
                filteredTopics = Sets.filter(allTopicNames, new IncludePatternPredicate(includeTopics));
            } else if (excludeTopics != null && !excludeTopics.isEmpty()) {
                filteredTopics = Sets.filter(allTopicNames, new ExcludePatternPredicate(excludeTopics));
            } else {
                //Fail safe
                filteredTopics = Sets.newHashSet();
            }
            logger.info("Filtered Topics are [{}]", filteredTopics.stream().collect(Collectors.joining(",")));

            for(String topicName : filteredTopics) {
                MetricsQueryOptions metricsQueryOptions = new MetricsQueryOptions();
                metricsQueryOptions.setFilter(String.format("EntityName eq '%s'", topicName));
                OffsetDateTime endTime = OffsetDateTime.now(ZoneId.of("UTC"));
                OffsetDateTime startTime = endTime.minusMinutes(1);
                QueryTimeInterval queryTimeInterval = new QueryTimeInterval(startTime, endTime);
                metricsQueryOptions.setTimeInterval(queryTimeInterval);
                metricsQueryOptions.setAggregations(AggregationType.AVERAGE);

                List<String> allMetrics = getAllMetricNames();
                MetricsQueryResult result = metricsQueryAsyncClient.queryResourceWithResponse(serviceBusResourceId, allMetrics, metricsQueryOptions).block().getValue();
                List<MetricResult> metrics = result.getMetrics();

                Map<String, String> topicMetrics = new HashMap<>();
                for(MetricResult metricResult : metrics) {
                    String metricName = metricResult.getMetricName();
                    Double metricValue = metricResult.getTimeSeries().stream().findFirst().get().getValues().stream().findFirst().get().getAverage();
                    topicMetrics.put(metricName, String.valueOf(metricValue));
                }

                SBTopicInner sbTopicInner = serviceBusManagementClient.getTopics().get(resourceGroup, namespace, topicName);
                Long transferMessageCount = sbTopicInner.countDetails().transferMessageCount();
                topicMetrics.put("TransferMessageCount", String.valueOf(transferMessageCount));

                Long transferDeadLetterMessageCount = sbTopicInner.countDetails().transferDeadLetterMessageCount();
                topicMetrics.put("TransferDeadLetterMessageCount", String.valueOf(transferDeadLetterMessageCount));

                Integer maxSizeInMegabytes = sbTopicInner.maxSizeInMegabytes();
                topicMetrics.put("MaxSizeInMegabytes", String.valueOf(maxSizeInMegabytes));

                String status = sbTopicInner.status().toString();
                topicMetrics.put("Status", status);

                processTopicMetrics(topicMetrics, topicMetricList, topicName);
            }
        } catch (Exception e) {
            logger.error("Error occurred while performing TopicMetricProcessor task", e);
        } finally {
            metricWriteHelper.transformAndPrintMetrics(topicMetricList);
            logger.info("Phaser arrived for Topic metric processor for server {}", displayName);
            phaser.arriveAndDeregister();
        }
    }

    private List<String> getAllMetricNames() {
        List<String> metricNames = topicMetricsFromConfig.stream().filter(topicMetric -> !Boolean.valueOf((String)topicMetric.get("fromTopic"))).map(topicMetric -> (String)topicMetric.get("name")).collect(Collectors.toList());
        return metricNames;
    }

    private String getServiceBusResourceId(Map<String, String> topicWithSBId) {
        String topicId = topicWithSBId.values().stream().findFirst().get();
        return topicId.substring(0, topicId.indexOf("/topics"));
    }

    public void processTopicMetrics(Map<String, String> topicMetrics, List<Metric> topicMetricList, String topicName) {
        for (Map topicMetricFromConfig : topicMetricsFromConfig) {
            String name = (String) topicMetricFromConfig.get("name");
            if (topicMetrics.containsKey(name) && topicMetrics.get(name) != null) {
                String value = topicMetrics.get(name);
                topicMetricList.add(new Metric(name, value, metricPrefix + SEPARATOR + "Topics" + SEPARATOR + topicName + SEPARATOR + topicMetricFromConfig.get("alias"), topicMetricFromConfig));
            }
        }
    }
}