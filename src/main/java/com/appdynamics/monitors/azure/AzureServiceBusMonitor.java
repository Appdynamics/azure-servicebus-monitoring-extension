package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.monitors.azure.config.*;
import com.appdynamics.monitors.azure.statsCollector.AzureServiceBusStatsCollector;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
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
                logger.info("Completed the GlassFish Monitoring Task successfully");
                return new TaskOutput("GlassFish Monitor executed successfully");
            } catch (FileNotFoundException e) {
                logger.error("Config File not found: " + configFilename, e);
            } catch (Exception e) {
                logger.error("Metrics Collection Failed: ", e);
            }
        }
        throw new TaskExecutionException("GlassFish Monitor completed with failures");
    }

    private void collectAndPrintMetrics(Configuration config) throws TaskExecutionException {
        Azure azure = config.getAzure();
        String metricPrefix = config.getMetricPrefix();
        List<Namespace> namespaces = config.getNamespaces();
        if (namespaces == null || namespaces.isEmpty()) {
            logger.info("No namespaces configured. Please configure namespaces in config.yml file to get stats");
            return;
        }
        for (Namespace namespace : namespaces) {
            String namespaceName = namespace.getNamespace();
            Set<String> queueNames = getQueueNames(config, namespaceName, namespace.getExcludeQueues());
            Set<String> topicNames = getTopicNames(config, namespaceName, namespace.getExcludeTopics());
            if (Strings.isNullOrEmpty(namespaceName)) {
                logger.info("No value for namespaces in configuration. Ignoring the entry");
                continue;
            }

            TaskExecutionException queueError = null;
            TaskExecutionException topicError = null;
            boolean queueStatsCollected = false;
            boolean topicStatsCollected = false;
            if (queueNames != null && !queueNames.isEmpty()) {
                try {
                    Map<String, String> queueStats = azureServiceBusStatsCollector.collectQueueStats(azure, namespaceName, queueNames, namespace.getQueueStats());
                    printMetrics(queueStats, metricPrefix);
                    queueStatsCollected = true;
                } catch (TaskExecutionException e) {
                    queueStatsCollected = false;
                    queueError = e;//Hold the exception until the process finishes execution
                }
            }

            if (topicNames != null && !topicNames.isEmpty()) {
                try {
                    Map<String, String> topicStats = azureServiceBusStatsCollector.collectTopicStats(azure, namespaceName, topicNames, namespace.getTopicStats());
                    printMetrics(topicStats, metricPrefix);
                    topicStatsCollected = true;
                } catch (TaskExecutionException e) {
                    topicStatsCollected = false;
                    topicError = e;//Hold the exception until the process finishes execution
                }
            }

            if (queueStatsCollected || topicStatsCollected) {
                print(metricPrefix + AzureServiceBusMonitorConstants.METRICS_COLLECTED, AzureServiceBusMonitorConstants.SUCCESS_VALUE);
            } else {
                print(metricPrefix + AzureServiceBusMonitorConstants.METRICS_COLLECTED, AzureServiceBusMonitorConstants.ERROR_VALUE);
            }

            //Check the errors and throw if any
            if (queueError != null) {
                throw queueError;
            } else if (topicError != null) {
                throw topicError;
            }

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
