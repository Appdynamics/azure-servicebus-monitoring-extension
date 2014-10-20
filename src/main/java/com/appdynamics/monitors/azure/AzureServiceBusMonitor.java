/**
 * Copyright 2014 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.monitors.azure.config.Azure;
import com.appdynamics.monitors.azure.config.ConfigUtil;
import com.appdynamics.monitors.azure.config.Configuration;
import com.appdynamics.monitors.azure.config.Namespace;
import com.appdynamics.monitors.azure.statsCollector.AzureServiceBusStatsCollector;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class AzureServiceBusMonitor extends AManagedMonitor {

    private static final Logger logger = Logger.getLogger(AzureServiceBusMonitor.class);

    public static final String METRICS_SEPARATOR = "|";
    private static final String CONFIG_ARG = "config-file";
    private static final String FILE_NAME = "monitors/AzureServiceBusMonitor/config.yml";

    private static final ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();
    private AzureServiceBusStatsCollector azureServiceBusStatsCollector = new AzureServiceBusStatsCollector();

    public AzureServiceBusMonitor() {
        String details = AzureServiceBusMonitor.class.getPackage().getImplementationTitle();
        String msg = "Using Monitor Version [" + details + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if (taskArgs != null) {
            logger.info("Starting the AzureServiceBus Monitoring task.");
            String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
            try {
                Configuration config = configUtil.readConfig(configFilename, Configuration.class);
                collectAndPrintMetrics(config);
                logger.info("Completed the AzureServiceBus Monitoring Task successfully");
                return new TaskOutput("AzureServiceBus Monitor executed successfully");
            } catch (FileNotFoundException e) {
                logger.error("Config File not found: " + configFilename, e);
            } catch (Exception e) {
                logger.error("Metrics Collection Failed: ", e);
            }
        }
        throw new TaskExecutionException("AzureServiceBus Monitor completed with failures");
    }

    private void collectAndPrintMetrics(final Configuration config) throws TaskExecutionException {
        final Azure azure = config.getAzure();
        final String metricPrefix = config.getMetricPrefix();
        List<Namespace> namespaces = config.getNamespaces();
        if (namespaces == null || namespaces.isEmpty()) {
            logger.info("No namespaces configured. Please configure namespaces in config.yml file to get stats");
            return;
        }

        ListeningExecutorService namespaceService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(config.getNamespaceThreads()));
        try {
            for (final Namespace namespace : namespaces) {

                final String namespaceName = namespace.getNamespace();
                if (Strings.isNullOrEmpty(namespaceName)) {
                    logger.info("No value for namespaces in configuration. Ignoring the entry");
                    continue;
                }

                //Queue Stats
                ListenableFuture<Set<String>> getQueueNames = namespaceService.submit(new Callable<Set<String>>() {
                    public Set<String> call() {
                        return getQueueNames(config, namespaceName, namespace.getExcludeQueues());
                    }
                });

                Futures.addCallback(getQueueNames, new FutureCallback<Set<String>>() {
                    public void onSuccess(Set<String> queueNames) {
                        if (queueNames != null && !queueNames.isEmpty()) {
                            try {
                                Map<String, String> queueStats = azureServiceBusStatsCollector.collectQueueStats(azure, namespaceName, queueNames, namespace.getQueueStats(), config.getQueueThreads());
                                printMetrics(queueStats, metricPrefix);
                            } catch (TaskExecutionException e) {
                                logger.error("Unable to get queue stats for namespace [" + namespaceName, e);
                            }
                        }
                    }

                    public void onFailure(Throwable thrown) {
                        logger.error("Unable to get queues for namespace [" + namespaceName, thrown);
                    }
                });

                //Topic stats
                ListenableFuture<Set<String>> getTopicNames = namespaceService.submit(new Callable<Set<String>>() {
                    public Set<String> call() {
                        return getTopicNames(config, namespaceName, namespace.getExcludeTopics());
                    }
                });

                Futures.addCallback(getTopicNames, new FutureCallback<Set<String>>() {
                    public void onSuccess(Set<String> topicNames) {
                        if (topicNames != null && !topicNames.isEmpty()) {
                            try {
                                Map<String, String> topicStats = azureServiceBusStatsCollector.collectTopicStats(azure, namespaceName, topicNames, namespace.getTopicStats(), config.getTopicThreads());
                                printMetrics(topicStats, metricPrefix);
                            } catch (TaskExecutionException e) {
                                logger.error("Unable to get topic stats for namespace [" + namespaceName, e);
                            }
                        }
                    }

                    public void onFailure(Throwable thrown) {
                        logger.error("Unable to get topics for namespace [" + namespaceName, thrown);
                    }
                });
            }
        } finally {
            namespaceService.shutdown();
        }
    }

    private void printMetrics(Map<String, String> resourceStats, String metricPrefix) {

        for (Map.Entry<String, String> statsEntry : resourceStats.entrySet()) {
            String value = statsEntry.getValue();
            String key = metricPrefix + statsEntry.getKey();
            try {
                double metricValue = Double.parseDouble(value.trim());
                print(key, metricValue);
            } catch (NumberFormatException e) {
                logger.error("Value of metric [" + key + "] can not be converted to number, Ignoring the stats.");
            }
        }

    }

    private void print(String key, double metricValue) {
        MetricWriter metricWriter = super.getMetricWriter(key, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
        metricWriter.printMetric(String.valueOf(Math.round(metricValue)));
    }

    private Set<String> getTopicNames(Configuration config, String namespaceName, Set<String> excludedTopicNames) {
        Set<String> topicNames = azureServiceBusStatsCollector.getTopicNames(config, namespaceName);
        if (excludedTopicNames == null) {
            return topicNames;
        }
        return Sets.difference(topicNames, excludedTopicNames);
    }

    private Set<String> getQueueNames(Configuration config, String namespaceName, Set<String> excludedQueueNames) {
        Set<String> queueNames = azureServiceBusStatsCollector.getQueueNames(config, namespaceName);
        if (excludedQueueNames == null) {
            return queueNames;
        }
        return Sets.difference(queueNames, excludedQueueNames);
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }

        if ("".equals(filename)) {
            filename = FILE_NAME;
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

    public static void main(String[] args) throws TaskExecutionException {
        AzureServiceBusMonitor azureServiceBusMonitor = new AzureServiceBusMonitor();
        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put(CONFIG_ARG, "/home/satish/AppDynamics/Code/extensions/azure-servicebus-monitoring-extension/src/main/resources/config/config.yml");
        azureServiceBusMonitor.execute(taskArgs, null);
    }
}
