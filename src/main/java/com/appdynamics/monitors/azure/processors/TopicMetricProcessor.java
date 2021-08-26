package com.appdynamics.monitors.azure.processors;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.azure.ExcludePatternPredicate;
import com.appdynamics.monitors.azure.IncludePatternPredicate;
import com.appdynamics.monitors.azure.Utility;
import com.appdynamics.monitors.azure.pojo.CountDetails;
import com.appdynamics.monitors.azure.pojo.Feed;
import com.appdynamics.monitors.azure.pojo.TopicDescription;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Phaser;
import java.util.stream.Collectors;

import static com.appdynamics.monitors.azure.Constants.SEPARATOR;

public class TopicMetricProcessor implements Runnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(TopicMetricProcessor.class);

    private CloseableHttpClient httpClient;
    private MetricWriteHelper metricWriteHelper;
    private XStream xStream;
    private String metricPrefix;
    private String displayName;
    private String namespace;
    private String serviceBusRootUri;
    private String plainSasKeyName;
    private String plainSasKey;
    private String endpoint;
    private List<String> includeTopics;
    private List<String> excludeTopics;
    private List<Map> topicMetricsFromConfig;
    private Phaser phaser;

    public TopicMetricProcessor(CloseableHttpClient httpClient, MetricWriteHelper metricWriteHelper, XStream xStream, String metricPrefix, String displayName, String namespace, String serviceBusRootUri, String plainSasKeyName, String plainSasKey, String endpoint, List<String> includeTopics, List<String> excludeTopics, List<Map> topicMetricsFromConfig, Phaser phaser) {
        this.httpClient = httpClient;
        this.metricWriteHelper = metricWriteHelper;
        this.xStream = xStream;
        this.metricPrefix = metricPrefix;
        this.displayName = displayName;
        this.namespace = namespace;
        this.serviceBusRootUri = serviceBusRootUri;
        this.plainSasKeyName = plainSasKeyName;
        this.plainSasKey = plainSasKey;
        this.endpoint = endpoint;
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
            String response = Utility.getStringResponseFromUrl(httpClient, namespace, serviceBusRootUri, plainSasKeyName, plainSasKey, endpoint);
            Feed topicFeed = (Feed) xStream.fromXML(response);
            List<TopicDescription> allElements = topicFeed.listTopics();

            Collection<TopicDescription> filteredTopics = null;

            if ((includeTopics == null || includeTopics.isEmpty()) && (excludeTopics == null || excludeTopics.isEmpty())) {
                filteredTopics = allElements;
            } else if (includeTopics != null && !includeTopics.isEmpty()) {
                filteredTopics = Collections2.filter(allElements, new IncludePatternPredicate(includeTopics));
            } else if (excludeTopics != null && !excludeTopics.isEmpty()) {
                filteredTopics = Collections2.filter(allElements, new ExcludePatternPredicate(excludeTopics));
            } else {
                //Fail safe
                filteredTopics = new ArrayList<>();
            }
            logger.debug("Filtered topics are [{}]",filteredTopics.stream().map(topicDesc -> topicDesc.getTitle()).collect(Collectors.joining(",")));

            for (TopicDescription topicInfo : filteredTopics) {

                Map<String, String> topicMetrics = new HashMap<>();

                CountDetails countDetails = topicInfo.getCountDetails();

                Long activeMessageCount = countDetails.getActiveMessageCount();
                topicMetrics.put("ActiveMessageCount", String.valueOf(activeMessageCount));

                Long deadLetterMessageCount = countDetails.getDeadLetterMessageCount();
                topicMetrics.put("DeadLetterMessageCount", String.valueOf(deadLetterMessageCount));

                Long scheduledMessageCount = countDetails.getScheduledMessageCount();
                topicMetrics.put("ScheduledMessageCount", String.valueOf(scheduledMessageCount));

                Long transferDeadLetterMessageCount = countDetails.getTransferDeadLetterMessageCount();
                topicMetrics.put("TransferDeadLetterMessageCount", String.valueOf(transferDeadLetterMessageCount));

                Long transferMessageCount = countDetails.getTransferMessageCount();
                topicMetrics.put("TransferMessageCount", String.valueOf(transferMessageCount));

                Long maxSizeInMegabytes = topicInfo.getMaxSizeInMegabytes();
                topicMetrics.put("MaxSizeInMegabytes", String.valueOf(maxSizeInMegabytes));

                Long sizeInBytes = topicInfo.getSizeInBytes();
                topicMetrics.put("SizeInBytes", String.valueOf(sizeInBytes));

                String status = topicInfo.getStatus();
                topicMetrics.put("Status", status);

                String availabilityStatus = topicInfo.getEntityAvailabilityStatus();
                topicMetrics.put("EntityAvailabilityStatus", availabilityStatus);

                processTopicMetrics(topicMetrics, topicMetricList, topicInfo.getTitle());
            }

        } catch (Exception e) {
            logger.error("Error occurred while performing TopicMetricProcessor task", e);
        } finally {
            metricWriteHelper.transformAndPrintMetrics(topicMetricList);
            logger.info("Phaser arrived for Topic metric processor for server {}", displayName);
            phaser.arriveAndDeregister();
        }
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