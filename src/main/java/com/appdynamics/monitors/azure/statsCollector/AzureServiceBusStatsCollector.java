package com.appdynamics.monitors.azure.statsCollector;

import com.appdynamics.monitors.azure.AzureServiceBusMonitor;
import com.appdynamics.monitors.azure.config.Azure;
import com.appdynamics.monitors.azure.config.Configuration;
import com.appdynamics.monitors.azure.metrics.*;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

public class AzureServiceBusStatsCollector {

    private static final Logger logger = Logger.getLogger(AzureServiceBusStatsCollector.class);

    public static final String REQUEST_METHOD_GET = "GET";
    public static final String X_MS_VERSION_HEADER = "x-ms-version";

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String X_MS_VERSION = "2013-11-01";

    private static final String QUEUES = "Queues";
    private static final String TOPICS = "Topics";

    private static final String STAT_URL = "https://management.core.windows.net/${SubscriptionId}/services/monitoring/metricvalues/query?resourceId=/servicebus/namespaces/${NameSpace}/${ResourceType}/${ResourceName}&names=${Stats}&timeGrain=PT5M&startTime=${StartTime}&endTime=${EndTime}";
    private static final String RESOURCE_NAMES_URL = "https://management.core.windows.net/${SubscriptionId}/services/servicebus/Namespaces/${NameSpace}/${ResourceType}";

    public Map<String, String> collectQueueStats(Azure azure, String namespaceName, Set<String> queueNames, Set<String> queueStats) throws TaskExecutionException {

        Map<String, String> metricMap = new HashMap<String, String>();
        Map<String, String> valueMap = createValueMap(azure, namespaceName, QUEUES, queueStats);

        for (String queueName : queueNames) {
            valueMap.put("ResourceName", queueName);
            try {
                getStatsFromAzure(azure, namespaceName, valueMap, queueName, QUEUES, metricMap);
            } catch (Exception e) {
                logger.error("Error getting stats for queue [" + namespaceName + "/" + queueName + "]", e);
                throw new TaskExecutionException("Error getting stats for queue [" + namespaceName + "/" + queueName + "]", e);
            }
        }
        return metricMap;
    }

    private void getStatsFromAzure(Azure azure, String namespaceName, Map<String, String> valueMap, String resourceName, String resourceType, Map<String, String> metricMap) throws IOException {
        StrSubstitutor strSubstitutor = new StrSubstitutor(valueMap);
        String statsUrlString = strSubstitutor.replace(STAT_URL);
        URL statsUrl = new URL(statsUrlString);

        InputStream is = processGetRequest(statsUrl, azure.getKeyStoreLocation(), azure.getKeyStorePassword());

        XStream xstream = new XStream();
        xstream.ignoreUnknownElements();
        xstream.processAnnotations(MetricValueSetCollection.class);
        xstream.processAnnotations(MetricValueSet.class);
        xstream.processAnnotations(MetricValue.class);
        MetricValueSetCollection metricValueSetCollection = (MetricValueSetCollection) xstream.fromXML(is);

        metricMap.putAll(extractMetrics(metricValueSetCollection, namespaceName, resourceType, resourceName));
    }

    public Map<String, String> collectTopicStats(Azure azure, String namespaceName, Set<String> topicNames, Set<String> topicStats) throws TaskExecutionException {

        Map<String, String> metricMap = new HashMap<String, String>();
        Map<String, String> valueMap = createValueMap(azure, namespaceName, TOPICS, topicStats);

        for (String topicName : topicNames) {
            valueMap.put("ResourceName", topicName);
            try {
                getStatsFromAzure(azure, namespaceName, valueMap, topicName, TOPICS, metricMap);
            } catch (Exception e) {
                logger.error("Error getting stats for topic [" + namespaceName + "/" + topicName + "]", e);
                throw new TaskExecutionException("Error getting stats for topic [" + namespaceName + "/" + topicName + "]", e);
            }
        }
        return metricMap;
    }

    private Map<String, String> createValueMap(Azure azure, String namespaceName, String resourceType, Set<String> queueStats) {
        Map<String, String> valueMap = new HashMap<String, String>();
        valueMap.put("SubscriptionId", azure.getSubscriptionId());
        valueMap.put("NameSpace", namespaceName);
        valueMap.put("ResourceType", resourceType);

        String stats = Joiner.on(",").skipNulls().join(queueStats);
        valueMap.put("Stats", stats);

        DateTime dateTime = new DateTime(DateTimeZone.UTC).minusMinutes(15);
        String endTime = dateTime.toString(DateTimeFormat.forPattern(DATE_FORMAT));
        String startTime = dateTime.minusMinutes(1).toString(DateTimeFormat.forPattern(DATE_FORMAT));
        valueMap.put("StartTime", startTime);
        valueMap.put("EndTime", endTime);
        return valueMap;
    }

    private Map<String, String> extractMetrics(MetricValueSetCollection metricValueSetCollection, String namespaceName, String resourceType, String resourceName) {
        Map<String, String> metrics = new HashMap<String, String>();
        List<MetricValueSet> metricValueSets = metricValueSetCollection.getMetricValueSets();
        for (MetricValueSet metricValueSet : metricValueSets) {
            StringBuilder keyBuilder = new StringBuilder(namespaceName);
            keyBuilder.append(AzureServiceBusMonitor.METRICS_SEPARATOR).append(resourceType)
                    .append(AzureServiceBusMonitor.METRICS_SEPARATOR).append(resourceName)
                    .append(AzureServiceBusMonitor.METRICS_SEPARATOR).append(metricValueSet.getDisplayName());

            List<MetricValue> metricValues = metricValueSet.getMetricValues();
            String value = "0";
            if (metricValues != null && !metricValues.isEmpty()) {
                MetricValue metricValue = metricValues.get(0);
                if ("size".equals(metricValueSet.getName()) || "length".equals(metricValueSet.getName())) {
                    String total = metricValue.getMaximum();
                    if (total != null) {
                        value = total;
                    }
                } else {
                    String total = metricValue.getTotal();
                    if (total != null) {
                        value = total;
                    }
                }
            }
            metrics.put(keyBuilder.toString(), value);
        }
        return metrics;
    }

    private InputStream processGetRequest(URL url, String keyStore, String keyStorePassword) {
        SSLSocketFactory sslFactory = getSSLSocketFactory(keyStore, keyStorePassword);
        HttpsURLConnection con = null;
        try {
            con = (HttpsURLConnection) url.openConnection();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        con.setSSLSocketFactory(sslFactory);
        try {
            con.setRequestMethod(REQUEST_METHOD_GET);
        } catch (ProtocolException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        con.addRequestProperty(X_MS_VERSION_HEADER, X_MS_VERSION);

        InputStream responseStream = null;
        try {
            responseStream = (InputStream) con.getContent();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        return responseStream;
    }

    private SSLSocketFactory getSSLSocketFactory(String keyStoreName, String password) {
        KeyStore ks = getKeyStore(keyStoreName, password);
        KeyManagerFactory keyManagerFactory = null;
        try {
            keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(ks, password.toCharArray());
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
            return context.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (KeyStoreException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (UnrecoverableKeyException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (KeyManagementException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private KeyStore getKeyStore(String keyStoreName, String password) {
        KeyStore ks = null;
        FileInputStream fis = null;
        try {
            ks = KeyStore.getInstance("JKS");
            char[] passwordArray = password.toCharArray();
            fis = new java.io.FileInputStream(keyStoreName);
            ks.load(fis, passwordArray);
            fis.close();

        } catch (CertificateException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (KeyStoreException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return ks;
    }

    public Set<String> getTopicNames(Configuration config, String namespaceName) {
        try {
            return getResourceNames(namespaceName, config, TOPICS);
        } catch (MalformedURLException e) {
            logger.error("Error getting topic names for [" + namespaceName + "]", e);
        }

        return Sets.newHashSet();
    }

    private Set<String> getResourceNames(String namespaceName, Configuration config, String resourceType) throws MalformedURLException {
        Map<String, String> valueMap = new HashMap<String, String>();
        Azure azure = config.getAzure();
        valueMap.put("SubscriptionId", azure.getSubscriptionId());
        valueMap.put("NameSpace", namespaceName);
        valueMap.put("ResourceType", resourceType);
        StrSubstitutor strSubstitutor = new StrSubstitutor(valueMap);
        String resourceNamesUrlString = strSubstitutor.replace(RESOURCE_NAMES_URL);
        URL resourceNamesUrl = new URL(resourceNamesUrlString);
        InputStream inputStream = processGetRequest(resourceNamesUrl, azure.getKeyStoreLocation(), azure.getKeyStorePassword());

        XStream xstream = new XStream();
        xstream.ignoreUnknownElements();
        xstream.processAnnotations(Feed.class);
        xstream.processAnnotations(Entry.class);
        Feed feed = (Feed) xstream.fromXML(inputStream);

        Set<String> topicNames = new HashSet<String>();
        List<Entry> entries = feed.getEntries();
        if (entries != null && !entries.isEmpty()) {
            for (Entry entry : entries) {
                topicNames.add(entry.getTitle());
            }
        }
        return topicNames;
    }

    public Set<String> getQueueNames(Configuration config, String namespaceName) {
        try {
            return getResourceNames(namespaceName, config, QUEUES);
        } catch (MalformedURLException e) {
            logger.error("Error getting queue names for [" + namespaceName + "]", e);
        }
        return Sets.newHashSet();

    }
}