/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.azure;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;

import com.appdynamics.extensions.util.StringUtils;
import com.appdynamics.extensions.azure.Metrics.MetricsCollector;
import com.appdynamics.extensions.azure.config.input.Stat;
import com.appdynamics.extensions.azure.namespaces.NamespacesInfo;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.Phaser;


public class AzureServiceBusMonitoringTask implements AMonitorTaskRunnable {

    private String metricPrefix;
    private MetricWriteHelper metricWriter;
    private Map namespace;
    private Boolean status = true;
    private String displayName;
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(AzureServiceBusMonitoringTask.class);
    private MonitorContextConfiguration configuration;

    private NamespacesInfo namespacesInfo;
    private Util util = new Util();

    AzureServiceBusMonitoringTask(TasksExecutionServiceProvider serviceProvider, MonitorContextConfiguration configuration, NamespacesInfo namespacesInfo) {
        this.configuration = configuration;
        this.namespacesInfo = namespacesInfo;
        this.metricWriter = serviceProvider.getMetricWriteHelper();
        this.metricPrefix = configuration.getMetricPrefix() + "|" + namespacesInfo.getDisplayName();
        this.displayName = namespacesInfo.getDisplayName();
    }


    @Override
    public void run() {

        try {
            Phaser phaser = new Phaser();
            phaser.register();

            Stat.Stats metricConfig = (Stat.Stats) configuration.getMetricsXml();
            String sasToken = util.getSASToken("https://" + namespacesInfo.getNamespace() + namespacesInfo.getServiceBusRootUri(), namespacesInfo.getSasKeyName(), namespacesInfo.getSasKey() );

            for(Stat stat: metricConfig.getStats()) {
                if(StringUtils.hasText(stat.getAlias()) && stat.getAlias().equalsIgnoreCase("Queues")) {
                    phaser.register();
                    MetricsCollector queueMetricsCollectorTask = new MetricsCollector(stat, configuration.getContext(), namespacesInfo, metricWriter, phaser, metricPrefix,sasToken, true);
                    configuration.getContext().getExecutorService().execute("queueMetricsCollectorTask", queueMetricsCollectorTask);
                    logger.debug("Registering QueueMetricCollectorTask phaser for {}", displayName);
                } else if(StringUtils.hasText(stat.getAlias()) && stat.getAlias().equalsIgnoreCase("Topics")) {
                    phaser.register();

                    MetricsCollector topicMetricsCollectorTask = new MetricsCollector(stat, configuration.getContext(), namespacesInfo, metricWriter, phaser, metricPrefix, sasToken, false);
                    configuration.getContext().getExecutorService().execute("MetricCollectorTask", topicMetricsCollectorTask );
                    logger.debug("Registering TopicMetricCollectorTask phaser for {}", displayName);
                }
            }

            phaser.arriveAndAwaitAdvance();

            logger.info("Completed the AzureServiceBus Metric Monitoring task");

        } catch (Exception e) {
            status = false;
            logger.error("Unexpected error while running the Azure service bus Monitor", e);
        } finally {
            if (status == true) {
                metricWriter.printMetric(metricPrefix +"|" + "HeartBeat", "1", "AVERAGE", "AVERAGE", "INDIVIDUAL");

            } else {
                metricWriter.printMetric(metricPrefix + "|" + "HeartBeat", "0", "AVERAGE", "AVERAGE", "INDIVIDUAL");
            }
        }
    }

    @Override
    public void onTaskComplete() {

    }
}