/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;

import org.slf4j.Logger;

import java.util.List;
import java.util.Map;


import static com.appdynamics.monitors.azure.Constants.*;

/**
 * @author Satish Muddam
 */
public class AzureServiceBusMonitor extends ABaseMonitor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(AzureServiceBusMonitor.class);
    private MonitorContextConfiguration contextConfiguration;
    private Map<String,?> configYml;

    @Override
    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return MONITOR_NAME;
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        contextConfiguration = getContextConfiguration();
        configYml = contextConfiguration.getConfigYml();
        AssertUtils.assertNotNull(configYml,"The config.yml cannot be null");
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        List<Map<String,?>> servers = (List<Map<String, ?>>) configYml.get(SERVERS);
        AssertUtils.assertNotNull(servers,"The servers section in config.yml cannot be null");
        for(Map<String,?> server: servers){
            AssertUtils.assertNotNull(server,"The server arguments cannot be empty ");
            logger.info("Starting AzureServiceBusMonitoringTask for server {}",server.get(DISPLAY_NAME));
            AzureServiceBusMonitoringTask task = new AzureServiceBusMonitoringTask(contextConfiguration, tasksExecutionServiceProvider.getMetricWriteHelper(), configYml, server);
            tasksExecutionServiceProvider.submit((String) server.get(DISPLAY_NAME),task);
        }
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        return (List<Map<String, ?>>) configYml.get(SERVERS);
    }
}