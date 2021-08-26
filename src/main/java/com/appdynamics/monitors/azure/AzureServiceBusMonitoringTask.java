/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.CryptoUtils;
import com.appdynamics.monitors.azure.pojo.Feed;
import com.appdynamics.monitors.azure.processors.QueueMetricProcessor;
import com.appdynamics.monitors.azure.processors.TopicMetricProcessor;
import com.thoughtworks.xstream.XStream;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

import static com.appdynamics.monitors.azure.Constants.*;

/**
 * @author Satish Muddam
 */
public class AzureServiceBusMonitoringTask implements AMonitorTaskRunnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(AzureServiceBusMonitoringTask.class);
    private MonitorContextConfiguration contextConfiguration;
    private MetricWriteHelper metricWriteHelper;
    private Map<String, ?> configYml;
    private Map<String, ?> server;
    private String metricPrefix;

    public AzureServiceBusMonitoringTask(MonitorContextConfiguration contextConfiguration, MetricWriteHelper metricWriteHelper, Map<String, ?> configYml, Map<String, ?> server) {
        this.contextConfiguration = contextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.configYml = configYml;
        this.server = server;
        this.metricPrefix = contextConfiguration.getMetricPrefix();
    }

    @Override
    public void run() {
        final String displayName = (String) server.get(DISPLAY_NAME);
        final String namespace = (String) server.get(NAMESPACE);
        final String sasKeyName = (String) server.get(SASKEYNAME);
        final String sasKey = (String) server.get(SASKEY);
        final String encryptedSasKeyName = (String) server.get(ENCRYPTEDSASKEYNAME);
        final String encryptedSasKey = (String) server.get(ENCRYPTEDSASKEY);
        final String serviceBusRootUri = (String) server.get(SERVICEBUSROOTURI);
        String encryptionKey = (String) configYml.get(ENCRYPTIONKEY);
        final String plainSasKeyName = decryptIfEncrypted(sasKeyName, encryptedSasKeyName, encryptionKey);
        final String plainSasKey = decryptIfEncrypted(sasKey, encryptedSasKey, encryptionKey);
        final List<String> includeQueues = (List<String>) server.get(INCLUDE_QUEUES);
        final List<String> excludeQueues = (List<String>) server.get(EXCLUDE_QUEUES);
        final List<String> includeTopics = (List<String>) server.get(INCLUDE_TOPICS);
        final List<String> excludeTopics = (List<String>) server.get(EXCLUDE_TOPICS);
        final List<Map> queueMetricsFromConfig = (List) configYml.get(QUEUE_METRICS);
        final List<Map> topicMetricsFromConfig = (List) configYml.get(TOPIC_METRICS);
        metricPrefix = metricPrefix + SEPARATOR + displayName;

        try {
            Phaser phaser = new Phaser();
            phaser.register();
            XStream xStream = initXStream();
            if (queueMetricsFromConfig != null && !queueMetricsFromConfig.isEmpty()) {
                String endpoint = configYml.get(QUEUE_URL) != null ? (String) configYml.get(QUEUE_URL) : DEFAULT_QUEUE_URL;
                QueueMetricProcessor queueMetricProcessorTask = new QueueMetricProcessor(contextConfiguration.getContext().getHttpClient(), metricWriteHelper, xStream, metricPrefix, displayName, namespace, serviceBusRootUri, plainSasKeyName, plainSasKey, endpoint, includeQueues, excludeQueues, queueMetricsFromConfig, phaser);
                contextConfiguration.getContext().getExecutorService().execute("QueueMetricProcessorTask", queueMetricProcessorTask);
            }
            if (topicMetricsFromConfig != null && !topicMetricsFromConfig.isEmpty()) {
                String endpoint = configYml.get(TOPIC_URL) != null ? (String) configYml.get(TOPIC_URL) : DEFAULT_TOPIC_URL;
                TopicMetricProcessor topicMetricProcessorTask = new TopicMetricProcessor(contextConfiguration.getContext().getHttpClient(), metricWriteHelper, xStream, metricPrefix, displayName, namespace, serviceBusRootUri, plainSasKeyName, plainSasKey, endpoint, includeTopics, excludeTopics, topicMetricsFromConfig, phaser);
                contextConfiguration.getContext().getExecutorService().execute("topicMetricProcessorTask", topicMetricProcessorTask);
            }
            phaser.arriveAndAwaitAdvance();
            logger.info("Completed AzureServiceBusMonitor task for server {}", displayName);

        } catch (Exception e) {
            logger.error("Error occurred while running task for server " + displayName, e);
        } finally {

        }
    }

    public XStream initXStream() {
        XStream xStream = new XStream();
        xStream.ignoreUnknownElements();
        xStream.processAnnotations(Feed.class);
        xStream.allowTypeHierarchy(Feed.class);
        return xStream;
    }

    private String decryptIfEncrypted(String nonEncryptedString, String encryptedString, String encryptionKey) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(PASSWORD, nonEncryptedString);
        map.put(ENCRYPTEDPASSWORD, encryptedString);
        map.put(ENCRYPTIONKEY, encryptionKey);
        return CryptoUtils.getPassword(map);
    }

    @Override
    public void onTaskComplete() {
        logger.info("Completed task for server {}", server.get(DISPLAY_NAME));
    }
}