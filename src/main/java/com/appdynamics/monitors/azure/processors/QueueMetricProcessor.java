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
import com.azure.resourcemanager.servicebus.fluent.models.SBQueueInner;
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

public class QueueMetricProcessor implements Runnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(QueueMetricProcessor.class);

    private ServiceBusManagementClient serviceBusManagementClient;

    private MetricsQueryAsyncClient metricsQueryAsyncClient;
    private MetricWriteHelper metricWriteHelper;
    private String metricPrefix;
    private String displayName;

    private String resourceGroup;

    private String namespace;
    private List<String> includeQueues;
    private List<String> excludeQueues;
    private List<Map> queueMetricsFromConfig;
    private Phaser phaser;


    public QueueMetricProcessor(ServiceBusManagementClient serviceBusManagementClient, MetricsQueryAsyncClient metricsQueryAsyncClient, MetricWriteHelper metricWriteHelper, String metricPrefix, String displayName, String resourceGroup, String namespace, List<String> includeQueues, List<String> excludeQueues, List<Map> queueMetricsFromConfig, Phaser phaser) {
        this.serviceBusManagementClient = serviceBusManagementClient;
        this.metricsQueryAsyncClient = metricsQueryAsyncClient;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix = metricPrefix;
        this.displayName = displayName;
        this.resourceGroup = resourceGroup;
        this.namespace = namespace;
        this.includeQueues = includeQueues;
        this.excludeQueues = excludeQueues;
        this.queueMetricsFromConfig = queueMetricsFromConfig;
        this.phaser = phaser;
        this.phaser.register();
    }

    @Override
    public void run() {

        List<Metric> queueMetricList = Lists.newArrayList();
        try {

            PagedIterable<SBQueueInner> queuesIterator = serviceBusManagementClient.getQueues().listByNamespace(resourceGroup, namespace);

            Map<String, String> queuesWithSBId = Flux.fromIterable(queuesIterator)
                    .collectMap(queueInner -> queueInner.name(),
                            queueInner -> queueInner.id()).block();

            if(queuesWithSBId == null || queuesWithSBId.size() <= 0) {
                logger.info("Could not find queues in resource group [{}] namespace [{}]", resourceGroup, namespace);
                logger.info("Phaser arrived for Queue metric processor for server {}", displayName);
                phaser.arriveAndDeregister();
                return;
            }

            Set<String> allQueueNames = queuesWithSBId.keySet();
            String serviceBusResourceId = getServiceBusResourceId(queuesWithSBId);

            Set<String> filteredQueues;
            if ((includeQueues == null || includeQueues.isEmpty()) && (excludeQueues == null || excludeQueues.isEmpty())) {
                filteredQueues = allQueueNames;
            } else if (includeQueues != null && !includeQueues.isEmpty()) {
                filteredQueues = Sets.filter(allQueueNames, new IncludePatternPredicate(includeQueues));
            } else if (excludeQueues != null && !excludeQueues.isEmpty()) {
                filteredQueues = Sets.filter(allQueueNames, new ExcludePatternPredicate(excludeQueues));
            } else {
                //Fail safe
                filteredQueues = Sets.newHashSet();
            }
            logger.info("Filtered Queues are [{}]", filteredQueues.stream().collect(Collectors.joining(",")));


            for(String queueName : filteredQueues) {
                MetricsQueryOptions metricsQueryOptions = new MetricsQueryOptions();
                metricsQueryOptions.setFilter(String.format("EntityName eq '%s'", queueName));
                OffsetDateTime endTime = OffsetDateTime.now(ZoneId.of("UTC"));
                OffsetDateTime startTime = endTime.minusMinutes(1);
                QueryTimeInterval queryTimeInterval = new QueryTimeInterval(startTime, endTime);
                metricsQueryOptions.setTimeInterval(queryTimeInterval);
                metricsQueryOptions.setAggregations(AggregationType.AVERAGE);

                List<String> allMetrics = getAllMetricNames();
                MetricsQueryResult result = metricsQueryAsyncClient.queryResourceWithResponse(serviceBusResourceId, allMetrics, metricsQueryOptions).block().getValue();
                List<MetricResult> metrics = result.getMetrics();

                Map<String, String> queueMetrics = new HashMap<>();
                for(MetricResult metricResult : metrics) {
                    String metricName = metricResult.getMetricName();
                    Long metricValue = metricResult.getTimeSeries().stream().findFirst().get().getValues().stream().findFirst().get().getAverage().longValue();
                    queueMetrics.put(metricName, String.valueOf(metricValue));
                }

                SBQueueInner sbQueueInner = serviceBusManagementClient.getQueues().get(resourceGroup, namespace, queueName);

                Long transferMessageCount = sbQueueInner.countDetails().transferMessageCount();
                queueMetrics.put("TransferMessageCount", String.valueOf(transferMessageCount));

                Long transferDeadLetterMessageCount = sbQueueInner.countDetails().transferDeadLetterMessageCount();
                queueMetrics.put("TransferDeadLetterMessageCount", String.valueOf(transferDeadLetterMessageCount));

                Integer maxDeliveryCount = sbQueueInner.maxDeliveryCount();
                queueMetrics.put("MaxDeliveryCount", String.valueOf(maxDeliveryCount));

                Integer maxSizeInMegabytes = sbQueueInner.maxSizeInMegabytes();
                queueMetrics.put("MaxSizeInMegabytes", String.valueOf(maxSizeInMegabytes));

                String status = sbQueueInner.status().toString();
                queueMetrics.put("Status", status);

                processQueueMetrics(queueMetrics, queueMetricList, queueName);
            }
        } catch (Exception e) {
            logger.error("Error occurred while performing QueueMetricProcessor task", e);
        } finally {
            metricWriteHelper.transformAndPrintMetrics(queueMetricList);
            logger.info("Phaser arrived for Queue metric processor for server {}", displayName);
            phaser.arriveAndDeregister();
        }
    }

    private List<String> getAllMetricNames() {
        List<String> metricNames = queueMetricsFromConfig.stream().filter(queueMetric -> !Boolean.valueOf((String)queueMetric.get("fromQueue"))).map(queueMetric -> (String) queueMetric.get("name")).collect(Collectors.toList());
        return metricNames;
    }

    private String getServiceBusResourceId(Map<String, String> queuesWithSBId) {
        String queueId = queuesWithSBId.values().stream().findFirst().get();
        return queueId.substring(0, queueId.indexOf("/queues"));
    }

    public void processQueueMetrics(Map<String, String> queueMetrics, List<Metric> queueMetricList, String queueName) {
        for (Map queueMetricFromConfig : queueMetricsFromConfig) {
            String name = (String) queueMetricFromConfig.get("name");
            if (queueMetrics.containsKey(name) && queueMetrics.get(name) != null) {
                String value = queueMetrics.get(name);
                queueMetricList.add(new Metric(name, value, metricPrefix + SEPARATOR + "Queues" + SEPARATOR + queueName + SEPARATOR + queueMetricFromConfig.get("alias"), queueMetricFromConfig));
            }
        }
    }
}
