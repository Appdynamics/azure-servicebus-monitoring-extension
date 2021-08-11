/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.azure;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.Constants;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.util.CryptoUtils;
import com.appdynamics.extensions.azure.config.input.Stat;
import com.appdynamics.extensions.azure.namespaces.Namespaces;
import com.appdynamics.extensions.azure.namespaces.NamespacesInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AzureServiceBusMonitor extends ABaseMonitor {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(AzureServiceBusMonitor.class);

    protected Namespaces namespaces = new Namespaces();

    @Override
    protected String getDefaultMetricPrefix() {
        return Constant.METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "Azure ServiceBus monitor";
    }

    private void initialiseNamespaces(Map<String, ?> configYml) {

        List<Map<String,?>> namespaces = (List<Map<String, ?>>) configYml.get("servers");
        if(namespaces!=null && namespaces.size()>0){
            int index = 0;
            NamespacesInfo[] namespaceToSet = new NamespacesInfo[namespaces.size()];
            for(Map<String,?> namespace : namespaces){
                NamespacesInfo info = new NamespacesInfo();
                if(Strings.isNullOrEmpty((String) namespace.get("displayName"))){
                    logger.error("Display name not mentioned for server ");
                    throw new RuntimeException("Display name not mentioned for server");
                }

                AssertUtils.assertNotNull(namespace.get("namespace"), "The 'namespace is not initialised");
                AssertUtils.assertNotNull(namespace.get("serviceBusRootUri"), "The 'serviceBusRootUri URI is not initialised");


                ObjectMapper mapper = new ObjectMapper();

                info = mapper.convertValue(namespace, NamespacesInfo.class);

                if(!Strings.isNullOrEmpty((String) namespace.get("sasKeyName"))){
                    info.setSasKeyName((String) namespace.get("sasKeyName"));
                } else if(!Strings.isNullOrEmpty((String) namespace.get("encryptedSasKeyName"))){
                        try {
                            Map<String, String> args = Maps.newHashMap();
                            args.put(Constants.ENCRYPTED_PASSWORD, (String)namespace.get("encryptedSasKeyName"));
                            args.put(Constants.ENCRYPTION_KEY, (String)configYml.get("encryptionKey"));
                            logger.debug("Decrypting the encrypted Sas Key Name");
                            info.setSasKeyName(CryptoUtils.getPassword(args));

                        } catch (IllegalArgumentException e) {
                            String msg = "Encryption Key not specified. Please set the value in config.yml.";
                            logger.error(msg);
                            throw new IllegalArgumentException(msg);
                        }
                    }


                if(!Strings.isNullOrEmpty((String) namespace.get("sasKey"))){
                    info.setSasKey((String) namespace.get("sasKey"));
                } else if(!Strings.isNullOrEmpty((String) namespace.get("encryptedSasKey"))){
                    try {
                        Map<String, String> args = Maps.newHashMap();
                        args.put(Constants.ENCRYPTED_PASSWORD, (String)namespace.get("encryptedSasKey"));
                        args.put(Constants.ENCRYPTION_KEY, (String)configYml.get("encryptionKey"));
                        logger.debug("Decrypting the encrypted Sas Key Name");
                        info.setSasKey(CryptoUtils.getPassword(args));

                    } catch (IllegalArgumentException e) {
                        String msg = "Encryption Key not specified. Please set the value in config.yml.";
                        logger.error(msg);
                        throw new IllegalArgumentException(msg);
                    }
                }

                namespaceToSet[index++] = info;
            }

            this.namespaces.setNamespaces(namespaceToSet);
        } else {
            logger.error("no namespaces configured");
        }

    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        this.getContextConfiguration().setMetricXml(args.get("metric-file"), Stat.Stats.class);
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {
        initialiseNamespaces(getContextConfiguration().getConfigYml());
        AssertUtils.assertNotNull(getContextConfiguration().getMetricsXml(), "Metrics xml not available");
        AssertUtils.assertNotNull(namespaces, "The 'namespaces' section in config.yml is not initialised");

        for (NamespacesInfo namespacesInfo : namespaces.getInstances()) {
            AzureServiceBusMonitoringTask task = new AzureServiceBusMonitoringTask(serviceProvider, this.getContextConfiguration(), namespacesInfo);
            AssertUtils.assertNotNull(namespacesInfo.getDisplayName(), "The displayName can not be null");
            serviceProvider.submit((String) namespacesInfo.getDisplayName(), task);
        }
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        return (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get("servers");
    }

    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);
        org.apache.log4j.Logger.getRootLogger().addAppender(ca);

        AzureServiceBusMonitor monitor = new AzureServiceBusMonitor();
        final Map<String, String> taskArgs = new HashMap<>();
        taskArgs.put("config-file", "src/main/resources/config/config.yml");
        taskArgs.put("metric-file", "src/main/resources/config/metrics.xml");

        monitor.execute(taskArgs, null);

    }
}
