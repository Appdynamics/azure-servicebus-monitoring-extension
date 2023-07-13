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
import com.appdynamics.monitors.azure.processors.QueueMetricProcessor;
import com.appdynamics.monitors.azure.processors.TopicMetricProcessor;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.http.policy.HttpLoggingPolicy;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.monitor.query.MetricsQueryAsyncClient;
import com.azure.monitor.query.MetricsQueryClientBuilder;
import com.azure.resourcemanager.servicebus.ServiceBusManager;
import com.azure.resourcemanager.servicebus.fluent.ServiceBusManagementClient;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        final String resourceGroup = (String) server.get(RESOURCEGROUP);
        final String tenantId = (String) server.get(TENANTID);
        final String subscriptionId = (String) server.get(SUBSCRIPTIONID);

        final String clientId = (String) server.get(CLIENTID);
        final String clientSecret = (String) server.get(CLIENTSECRET);

        final String encryptedTenantId = (String) server.get(ENCRYPTEDTENANTID);
        final String encryptedSubscriptionId = (String) server.get(ENCRYPTEDSUBSCRIPTIONID);

        final String encryptedClientId = (String) server.get(ENCRYPTEDCLIENTID);
        final String encryptedClientSecret = (String) server.get(ENCRYPTEDCLIENTSECRET);

        String encryptionKey = (String) configYml.get(ENCRYPTIONKEY);
        final String plainTenantId = decryptIfEncrypted(tenantId, encryptedTenantId, encryptionKey);
        final String plainSubscriptionId = decryptIfEncrypted(subscriptionId, encryptedSubscriptionId, encryptionKey);
        final String plainClientId = decryptIfEncrypted(clientId, encryptedClientId, encryptionKey);
        final String plainClientSecret = decryptIfEncrypted(clientSecret, encryptedClientSecret, encryptionKey);
        final List<String> includeQueues = (List<String>) server.get(INCLUDE_QUEUES);
        final List<String> excludeQueues = (List<String>) server.get(EXCLUDE_QUEUES);
        final List<String> includeTopics = (List<String>) server.get(INCLUDE_TOPICS);
        final List<String> excludeTopics = (List<String>) server.get(EXCLUDE_TOPICS);
        final List<Map> queueMetricsFromConfig = (List) configYml.get(QUEUE_METRICS);
        final List<Map> topicMetricsFromConfig = (List) configYml.get(TOPIC_METRICS);
        metricPrefix = metricPrefix + SEPARATOR + displayName;
        Phaser phaser = new Phaser();
        try {
            ClientSecretCredential credential = getClientSecretCredential(plainTenantId, plainClientId, plainClientSecret);

            ServiceBusManagementClient serviceBusManagementClient = setupServiceBusClient(plainTenantId, plainSubscriptionId, credential);

            MetricsQueryAsyncClient metricsQueryAsyncClient = getMetricsQueryClientBuilder(credential);

            phaser.register();
            if (queueMetricsFromConfig != null && !queueMetricsFromConfig.isEmpty()) {
                QueueMetricProcessor queueMetricProcessorTask = new QueueMetricProcessor(serviceBusManagementClient, metricsQueryAsyncClient, metricWriteHelper, metricPrefix, displayName, resourceGroup, namespace, includeQueues, excludeQueues, queueMetricsFromConfig, phaser);
                contextConfiguration.getContext().getExecutorService().execute("QueueMetricProcessorTask", queueMetricProcessorTask);
            }
            if (topicMetricsFromConfig != null && !topicMetricsFromConfig.isEmpty()) {
                TopicMetricProcessor topicMetricProcessorTask = new TopicMetricProcessor(serviceBusManagementClient, metricsQueryAsyncClient, metricWriteHelper, metricPrefix, displayName, resourceGroup, namespace, includeTopics, excludeTopics, topicMetricsFromConfig, phaser);
                contextConfiguration.getContext().getExecutorService().execute("TopicMetricProcessorTask", topicMetricProcessorTask);
            }
            phaser.arriveAndAwaitAdvance();
            logger.info("Completed AzureServiceBusMonitor task for server {}", displayName);

        } catch (Exception e) {
            logger.error("Error occurred while running task for server " + displayName, e);
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    private MetricsQueryAsyncClient getMetricsQueryClientBuilder(ClientSecretCredential credential) {
        MetricsQueryClientBuilder metricsQueryClientBuilder = new MetricsQueryClientBuilder()
                .credential(credential)
                .httpClient(getHttpClient())
                .addPolicy(new HttpLoggingPolicy(getHttpLogOptions()));
        return metricsQueryClientBuilder.buildAsyncClient();
    }

    private ClientSecretCredential getClientSecretCredential(String plainTenantId, String plainClientId, String plainClientSecret) {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(plainTenantId)
                .clientId(plainClientId)
                .clientSecret(plainClientSecret)
                .httpPipeline(createHttpPipeline()).build();
        return credential;
    }

    private ServiceBusManagementClient setupServiceBusClient(String tenantId, String subscriptionId, ClientSecretCredential credential) {
        AzureEnvironment environment = AzureEnvironment.AZURE;
        AzureProfile azureProfile = new AzureProfile(tenantId, subscriptionId, environment);

       try {
           return ServiceBusManager.authenticate(credential, azureProfile).serviceClient();
       } catch(Exception e) {
           throw new RuntimeException("Error authenticating with Azure", e);
        }
    }

    private HttpPipeline createHttpPipeline() {
        HttpPipelineBuilder httpPipelineBuilder = new HttpPipelineBuilder();
        httpPipelineBuilder.policies(new HttpLoggingPolicy(getHttpLogOptions()));

        return httpPipelineBuilder.build();
    }

    private HttpClient getHttpClient() {
        return new NettyAsyncHttpClientBuilder()
                .connectionProvider(httpConnectionProvider())
                .responseTimeout(Duration.ofSeconds(30))
                .build();
    }

    private ConnectionProvider httpConnectionProvider() {
        return ConnectionProvider.builder("azure-servicebus-extension")
                .maxConnections(75)
                .maxIdleTime(Duration.ofSeconds(60))
                .pendingAcquireMaxCount(-1)
                .pendingAcquireTimeout(
                        Duration.ofSeconds(90))
                .build();
    }

    private HttpLogOptions getHttpLogOptions() {
        HttpLogOptions options = new HttpLogOptions();
        //Change log level to BODY_AND_HEADERS to see body responses for developer purposes.
        //Do not ship BODY_AND_HEADERS to prod as it will result in customer secrets being logged
        //in splunk.
        options.setLogLevel(HttpLogDetailLevel.HEADERS);
        options.setAllowedQueryParamNames(LOGGING_QUERY_PARAMETER_NAMES_WHITELIST);
        options.setAllowedHeaderNames(LOGGING_HEADER_NAMES_WHITELIST);
        return options;
    }

    private static final Set<String> LOGGING_HEADER_NAMES_WHITELIST = Sets.newHashSet(
            "x-ms-client-request-id",
            "x-ms-return-client-request-id",
            "traceparent",
            "Accept",
            "Cache-Control",
            "Connection",
            "Content-Length",
            "Content-Type",
            "Date",
            "ETag",
            "Expires",
            "If-Match",
            "If-Modified-Since",
            "If-None-Match",
            "If-Unmodified-Since",
            "Last-Modified",
            "Pragma",
            "Request-Id",
            "Retry-After",
            "Server",
            "Transfer-Encoding",
            "User-Agent",
            "x-ms-correlation-request-id",
            "x-ms-ratelimit-remaining-subscription-reads",
            "x-ms-ratelimit-remaining-resource",
            "x-ms-request-id",
            "x-ms-routing-request-id"
    );

    private static final Set<String> LOGGING_QUERY_PARAMETER_NAMES_WHITELIST = Sets.newHashSet(
            "timespan",
            "interval",
            "aggregation",
            "resultType",
            "api-version",
            "$skiptoken"
    );

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