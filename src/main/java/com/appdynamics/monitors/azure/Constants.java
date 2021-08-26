package com.appdynamics.monitors.azure;

public class Constants {
    public static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|Azure Service Bus";
    public static final String MONITOR_NAME = "AzureServiceBusMonitor";
    public static final String SEPARATOR = "|";

    //config
    public static final String QUEUE_METRICS = "queueMetrics";
    public static final String TOPIC_METRICS = "topicMetrics";
    public static final String QUEUE_URL = "queueUrl";
    public static final String TOPIC_URL = "topicUrl";
    public static final String DEFAULT_QUEUE_URL = "/$Resources/Queues?api-version=2013-07";
    public static final String DEFAULT_TOPIC_URL = "/$Resources/Topics?api-version=2013-07";

    ////For encryption
    public static final String PASSWORD = "password";
    public static final String ENCRYPTEDPASSWORD = "encryptedPassword";
    public static final String ENCRYPTIONKEY = "encryptionKey";

    //servers
    public static final String SERVERS = "servers";
    public static final String DISPLAY_NAME = "displayName";
    public static final String NAMESPACE = "namespace";
    public static final String SASKEYNAME = "sasKeyName";
    public static final String SASKEY = "sasKey";
    public static final String ENCRYPTEDSASKEYNAME = "encryptedSasKeyName";
    public static final String ENCRYPTEDSASKEY = "encryptedSasKey";
    public static final String SERVICEBUSROOTURI = "serviceBusRootUri";
    public static final String INCLUDE_QUEUES = "includeQueues";
    public static final String EXCLUDE_QUEUES = "excludeQueues";
    public static final String INCLUDE_TOPICS = "includeTopics";
    public static final String EXCLUDE_TOPICS = "excludeTopics";

}
