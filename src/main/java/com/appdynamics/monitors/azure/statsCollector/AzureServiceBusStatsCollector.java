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
package com.appdynamics.monitors.azure.statsCollector;

import com.appdynamics.monitors.azure.AzureServiceBusMonitor;
import com.appdynamics.monitors.azure.config.Azure;
import com.appdynamics.monitors.azure.config.Configuration;
import com.appdynamics.monitors.azure.metrics.Entry;
import com.appdynamics.monitors.azure.metrics.Feed;
import com.appdynamics.monitors.azure.metrics.MetricValue;
import com.appdynamics.monitors.azure.metrics.MetricValueSet;
import com.appdynamics.monitors.azure.metrics.MetricValueSetCollection;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

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

    public Map<String, String> collectQueueStats(final Azure azure, final String namespaceName, Set<String> queueNames, Set<String> queueStats, int queueThreads) throws TaskExecutionException {

        final Map<String, String> valueMap = createValueMap(azure, namespaceName, QUEUES, queueStats);

        ListeningExecutorService queueService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(queueThreads));
        final Map<String, String> queueMetricMap = new HashMap<String, String>();
        final CountDownLatch countDownLatch = new CountDownLatch(queueNames.size());

        try {
            for (final String queueName : queueNames) {
                valueMap.put("ResourceName", queueName);
                try {
                    ListenableFuture<Map<String, String>> getQueueNames = queueService.submit(new Callable<Map<String, String>>() {
                        public Map<String, String> call() throws IOException {
                            return getStatsFromAzure(azure, namespaceName, valueMap, queueName, QUEUES);
                        }
                    });

                    Futures.addCallback(getQueueNames, new FutureCallback<Map<String, String>>() {
                        public void onSuccess(Map<String, String> queueStats) {
                            countDownLatch.countDown();
                            queueMetricMap.putAll(queueStats);
                        }

                        public void onFailure(Throwable thrown) {
                            countDownLatch.countDown();
                            logger.error("Unable to get stats for queue [" + queueName + "] in namespace [" + namespaceName + "]", thrown);
                        }
                    });

                } catch (Exception e) {
                    logger.error("Error getting stats for queue [" + namespaceName + "/" + queueName + "]", e);
                    throw new TaskExecutionException("Error getting stats for queue [" + namespaceName + "/" + queueName + "]", e);
                }
            }
        } finally {
            queueService.shutdown();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error("Unable to wait till getting the queue stats", e);
        }
        return queueMetricMap;
    }

    private Map<String, String> getStatsFromAzure(Azure azure, String namespaceName, Map<String, String> valueMap, String resourceName, String resourceType) throws IOException {
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

        return extractMetrics(metricValueSetCollection, namespaceName, resourceType, resourceName);
    }

    public Map<String, String> collectTopicStats(final Azure azure, final String namespaceName, Set<String> topicNames, Set<String> topicStats, int topicThreads) throws TaskExecutionException {

        final Map<String, String> valueMap = createValueMap(azure, namespaceName, TOPICS, topicStats);

        ListeningExecutorService topicService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(topicThreads));
        final Map<String, String> topicMetricMap = new ConcurrentHashMap<String, String>();
        final CountDownLatch countDownLatch = new CountDownLatch(topicNames.size());

        try {
            for (final String topicName : topicNames) {
                valueMap.put("ResourceName", topicName);
                try {
                    ListenableFuture<Map<String, String>> getQueueNames = topicService.submit(new Callable<Map<String, String>>() {
                        public Map<String, String> call() throws IOException {
                            return getStatsFromAzure(azure, namespaceName, valueMap, topicName, TOPICS);
                        }
                    });

                    Futures.addCallback(getQueueNames, new FutureCallback<Map<String, String>>() {
                        public void onSuccess(Map<String, String> queueStats) {
                            countDownLatch.countDown();
                            topicMetricMap.putAll(queueStats);
                        }

                        public void onFailure(Throwable thrown) {
                            countDownLatch.countDown();
                            logger.error("Unable to get stats for topic [" + topicName + "] in namespace [" + namespaceName + "]", thrown);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error getting stats for topic [" + namespaceName + "/" + topicName + "]", e);
                    throw new TaskExecutionException("Error getting stats for topic [" + namespaceName + "/" + topicName + "]", e);
                }
            }
        } finally {
            topicService.shutdown();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error("Unable to wait till getting the topic stats", e);
        }
        return topicMetricMap;
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