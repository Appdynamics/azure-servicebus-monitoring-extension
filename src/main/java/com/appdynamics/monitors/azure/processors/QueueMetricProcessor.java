package com.appdynamics.monitors.azure.processors;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.azure.ExcludePatternPredicate;
import com.appdynamics.monitors.azure.IncludePatternPredicate;
import com.appdynamics.monitors.azure.Utility;
import com.appdynamics.monitors.azure.pojo.CountDetails;
import com.appdynamics.monitors.azure.pojo.Feed;
import com.appdynamics.monitors.azure.pojo.QueueDescription;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Phaser;
import java.util.stream.Collectors;

import static com.appdynamics.monitors.azure.Constants.SEPARATOR;

public class QueueMetricProcessor implements Runnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(QueueMetricProcessor.class);

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
    private List<String> includeQueues;
    private List<String> excludeQueues;
    private List<Map> queueMetricsFromConfig;
    private Phaser phaser;


    public QueueMetricProcessor(CloseableHttpClient httpClient, MetricWriteHelper metricWriteHelper, XStream xStream, String metricPrefix, String displayName, String namespace, String serviceBusRootUri, String plainSasKeyName, String plainSasKey, String endpoint, List<String> includeQueues, List<String> excludeQueues, List<Map> queueMetricsFromConfig, Phaser phaser) {
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
            String response = Utility.getStringResponseFromUrl(httpClient, namespace, serviceBusRootUri, plainSasKeyName, plainSasKey, endpoint);
            Feed queueFeed = (Feed) xStream.fromXML(response);
            List<QueueDescription> allElements = queueFeed.listQueues();

            Collection<QueueDescription> filteredQueues;
            if ((includeQueues == null || includeQueues.isEmpty()) && (excludeQueues == null || excludeQueues.isEmpty())) {
                filteredQueues = allElements;
            } else if (includeQueues != null && !includeQueues.isEmpty()) {
                filteredQueues = Collections2.filter(allElements, new IncludePatternPredicate(includeQueues));
            } else if (excludeQueues != null && !excludeQueues.isEmpty()) {
                filteredQueues = Collections2.filter(allElements, new ExcludePatternPredicate(excludeQueues));
            } else {
                //Fail safe
                filteredQueues = new ArrayList<>();
            }
            logger.debug("Filtered Queues are [{}]", filteredQueues.stream().map(x -> x.getTitle()).collect(Collectors.joining(",")));

            for (QueueDescription queueInfo : filteredQueues) {
                Map<String, String> queueMetrics = new HashMap<>();

                CountDetails countDetails = queueInfo.getCountDetails();

                Long activeMessageCount = countDetails.getActiveMessageCount();
                queueMetrics.put("ActiveMessageCount", String.valueOf(activeMessageCount));

                Long deadLetterMessageCount = countDetails.getDeadLetterMessageCount();
                queueMetrics.put("DeadLetterMessageCount", String.valueOf(deadLetterMessageCount));

                Long scheduledMessageCount = countDetails.getScheduledMessageCount();
                queueMetrics.put("ScheduledMessageCount", String.valueOf(scheduledMessageCount));

                Long transferDeadLetterMessageCount = countDetails.getTransferDeadLetterMessageCount();
                queueMetrics.put("TransferDeadLetterMessageCount", String.valueOf(transferDeadLetterMessageCount));

                Long transferMessageCount = countDetails.getTransferMessageCount();
                queueMetrics.put("TransferMessageCount", String.valueOf(transferMessageCount));

                Long maxDeliveryCount = queueInfo.getMaxDeliveryCount();
                queueMetrics.put("MaxDeliveryCount", String.valueOf(maxDeliveryCount));

                Long maxSizeInMegabytes = queueInfo.getMaxSizeInMegabytes();
                queueMetrics.put("MaxSizeInMegabytes", String.valueOf(maxSizeInMegabytes));

                Long messageCount = queueInfo.getMessageCount();
                queueMetrics.put("MessageCount", String.valueOf(messageCount));

                Long sizeInBytes = queueInfo.getSizeInBytes();
                queueMetrics.put("SizeInBytes", String.valueOf(sizeInBytes));

                String status = queueInfo.getStatus();
                queueMetrics.put("Status", status);

                String availabilityStatus = queueInfo.getEntityAvailabilityStatus();
                queueMetrics.put("EntityAvailabilityStatus", availabilityStatus);

                processQueueMetrics(queueMetrics, queueMetricList, queueInfo.getTitle());
            }

        } catch (Exception e) {
            logger.error("Error occurred while performing QueueMetricProcessor task", e);
        } finally {
            metricWriteHelper.transformAndPrintMetrics(queueMetricList);
            logger.info("Phaser arrived for Queue metric processor for server {}", displayName);
            phaser.arriveAndDeregister();
        }
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
