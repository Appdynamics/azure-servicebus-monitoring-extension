package com.appdynamics.monitors.azure;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.http.Http4ClientBuilder;
import com.appdynamics.monitors.azure.pojo.CountDetails;
import com.appdynamics.monitors.azure.pojo.Description;
import com.appdynamics.monitors.azure.pojo.Feed;
import com.appdynamics.monitors.azure.pojo.QueueDescription;
import com.appdynamics.monitors.azure.pojo.TopicDescription;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.io.BaseEncoding;
import com.thoughtworks.xstream.XStream;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Satish Muddam
 */
public class AzureServiceBusMonitoringTask implements Runnable {

    private static final Logger logger = Logger.getLogger(AzureServiceBusMonitoringTask.class);

    private MonitorConfiguration configuration;
    private Map azureConfiguration;
    private XStream xStream;

    private CloseableHttpClient httpClient;

    public AzureServiceBusMonitoringTask(MonitorConfiguration configuration, Map azureConfiguration, XStream xStream) {
        this.configuration = configuration;
        this.azureConfiguration = azureConfiguration;
        this.xStream = xStream;
    }

    @Override
    public void run() {

        final String namespace = (String) azureConfiguration.get("namespace");
        final String sasKeyName = (String) azureConfiguration.get("sasKeyName");
        final String sasKey = (String) azureConfiguration.get("sasKey");
        final String encryptedSasKeyName = (String) azureConfiguration.get("encryptedSasKeyName");
        final String encryptedSasKey = (String) azureConfiguration.get("encryptedSasKey");
        final String serviceBusRootUri = (String) azureConfiguration.get("serviceBusRootUri");

        Map<String, ?> configYml = configuration.getConfigYml();

        String encryptionKey = (String) configYml.get("encryptionKey");


        final String plainSasKeyName = decryptIfEncrypted(sasKeyName, encryptedSasKeyName, encryptionKey);
        final String plainSasKey = decryptIfEncrypted(sasKey, encryptedSasKey, encryptionKey);

        final List<String> includeQueues = (List<String>) azureConfiguration.get("includeQueues");
        final List<String> excludeQueues = (List<String>) azureConfiguration.get("excludeQueues");
        final List<String> includeTopics = (List<String>) azureConfiguration.get("includeTopics");
        final List<String> excludeTopics = (List<String>) azureConfiguration.get("excludeTopics");

        final List<Map> queueMetricsFromConfig = (List) configuration.getConfigYml().get("queueMetrics");
        final List<Map> topicMetricsFromConfig = (List) configuration.getConfigYml().get("topicMetrics");


        ExecutorService executorService = Executors.newFixedThreadPool(2);

        final String uri = "https://" + namespace + serviceBusRootUri;

        final String sasToken = getSASToken(uri, plainSasKeyName, plainSasKey);

        setupHttpClient(uri);

        executorService.submit(new Runnable() {
            @Override
            public void run() {

                CloseableHttpResponse response = null;
                try {

                    String requestURL = uri + "/$Resources/Queues?api-version=2013-07";
                    HttpGet get = new HttpGet(requestURL);
                    get.setHeader("Authorization", sasToken);

                    response = httpClient.execute(get);

                    StatusLine statusLine;

                    if (response != null && (statusLine = response.getStatusLine()) != null && statusLine.getStatusCode() == 200) {

                        String responseString = EntityUtils.toString(response.getEntity());

                        logger.trace("Queues response from azure " + responseString);
                        Feed queuesFeed = (Feed) xStream.fromXML(responseString);

                        parseQueueResult(namespace, queuesFeed.listQueues(), includeQueues, excludeQueues, queueMetricsFromConfig);

                    } else {
                        if (response != null) {
                            logger.error(String.format("The status line for the url [{}] is [{}] and the headers are [{}]", new Object[]{requestURL, response.getStatusLine(), response.getAllHeaders()}));
                            HttpEntity entity = response.getEntity();
                            if (entity != null) {
                                try {
                                    logger.error(String.format("The contents are {}", EntityUtils.toString(response.getEntity())));
                                } catch (Exception var4) {
                                    logger.error("", var4);
                                }
                            } else {
                                logger.error("The response content is null");
                            }
                        } else {
                            logger.error(String.format("The response is null for the URL {}", requestURL));
                        }
                    }


                } catch (Exception se) {
                    logger.error("Unexpected error while trying to get the queues", se);
                } finally {
                    if (response != null) {
                        try {
                            response.close();
                        } catch (IOException e) {
                            logger.debug("Error closing the response ", e);
                        }
                    }
                }
            }
        });

        executorService.submit(new Runnable() {
            @Override
            public void run() {

                CloseableHttpResponse response = null;
                try {

                    String requestURL = uri + "/$Resources/Topics?api-version=2013-07";
                    HttpGet get = new HttpGet(requestURL);
                    get.setHeader("Authorization", sasToken);

                    response = httpClient.execute(get);

                    StatusLine statusLine;

                    if (response != null && (statusLine = response.getStatusLine()) != null && statusLine.getStatusCode() == 200) {

                        String responseString = EntityUtils.toString(response.getEntity());
                        logger.trace("Topic response from azure " + responseString);
                        Feed topicsFeed = (Feed) xStream.fromXML(responseString);

                        parseTopicResult(namespace, topicsFeed.listTopics(), excludeTopics, topicMetricsFromConfig);

                    } else {
                        if (response != null) {
                            logger.error(String.format("The status line for the url [{}] is [{}] and the headers are [{}]", new Object[]{requestURL, response.getStatusLine(), response.getAllHeaders()}));
                            HttpEntity entity = response.getEntity();
                            if (entity != null) {
                                try {
                                    logger.error(String.format("The contents are {}", EntityUtils.toString(response.getEntity())));
                                } catch (Exception var4) {
                                    logger.error("", var4);
                                }
                            } else {
                                logger.error("The response content is null");
                            }
                        } else {
                            logger.error(String.format("The response is null for the URL {}", requestURL));
                        }
                    }

                } catch (Exception se) {
                    logger.error("Unexpected error while trying to get the topics", se);
                } finally {
                    if (response != null) {
                        try {
                            response.close();
                        } catch (IOException e) {
                            logger.debug("Error closing the response ", e);
                        }
                    }
                }
            }
        });
    }

    private void parseQueueResult(String namespace, List<QueueDescription> queues, List<String> includeQueues, List<String> excludeQueues, List<Map> queueMetricsFromConfig) {

        Collection<QueueDescription> filteredQueues = null;

        if ((includeQueues == null || includeQueues.isEmpty()) && (excludeQueues == null || excludeQueues.isEmpty())) {
            filteredQueues = queues;
        } else if (includeQueues != null && !includeQueues.isEmpty()) {
            filteredQueues = (Collection<QueueDescription>) includeConfigured(queues, includeQueues);
        } else if (excludeQueues != null && !excludeQueues.isEmpty()) {
            filteredQueues = (Collection<QueueDescription>) excludeConfigured(queues, excludeQueues);
        } else {
            //Fail safe
            filteredQueues = new ArrayList<QueueDescription>();
        }


        List<Metric> metrics = new ArrayList<Metric>();

        for (Map queueMetricFromConfig : queueMetricsFromConfig) {

            String metricName = (String) queueMetricFromConfig.get("name");
            String type = (String) queueMetricFromConfig.get("type");
            Map<String, String> converter = (Map<String, String>) queueMetricFromConfig.get("converter");

            Metric metric = new Metric();
            metric.setName(metricName);
            metric.setType(type);
            metric.setConverter(converter);
            metric.setPrefix(configuration.getMetricPrefix() + "|" + namespace + "|Queues|");

            metrics.add(metric);
        }

        for (QueueDescription queueInfo : filteredQueues) {

            Map<String, BigDecimal> queueMetrics = new HashMap<String, BigDecimal>();

            CountDetails countDetails = queueInfo.getCountDetails();

            Long activeMessageCount = countDetails.getActiveMessageCount();
            queueMetrics.put("ActiveMessageCount", new BigDecimal(activeMessageCount));

            Long deadLetterMessageCount = countDetails.getDeadLetterMessageCount();
            queueMetrics.put("DeadLetterMessageCount", new BigDecimal(deadLetterMessageCount));

            Long scheduledMessageCount = countDetails.getScheduledMessageCount();
            queueMetrics.put("ScheduledMessageCount", new BigDecimal(scheduledMessageCount));

            Long transferDeadLetterMessageCount = countDetails.getTransferDeadLetterMessageCount();
            queueMetrics.put("TransferDeadLetterMessageCount", new BigDecimal(transferDeadLetterMessageCount));

            Long transferMessageCount = countDetails.getTransferMessageCount();
            queueMetrics.put("TransferMessageCount", new BigDecimal(transferMessageCount));

            Long maxDeliveryCount = queueInfo.getMaxDeliveryCount();
            queueMetrics.put("MaxDeliveryCount", new BigDecimal(maxDeliveryCount));

            Long maxSizeInMegabytes = queueInfo.getMaxSizeInMegabytes();
            queueMetrics.put("MaxSizeInMegabytes", new BigDecimal(maxSizeInMegabytes));

            Long messageCount = queueInfo.getMessageCount();
            queueMetrics.put("MessageCount", new BigDecimal(messageCount));

            Long sizeInBytes = queueInfo.getSizeInBytes();
            queueMetrics.put("SizeInBytes", new BigDecimal(sizeInBytes));

            String status = queueInfo.getStatus();

            String availabilityStatus = queueInfo.getEntityAvailabilityStatus();

            printMetrics(queueInfo.getTitle(), metrics, queueMetrics, status, availabilityStatus);
        }
    }

    private void printMetrics(String destinationName, List<Metric> metrics, Map<String, BigDecimal> metricNameandValue, String status, String availabilityStatus) {

        for (Metric metric : metrics) {

            String metricName = metric.getName();
            BigDecimal metricValue = null;

            if ("Status".equals(metricName)) {

                String convert = metric.convert(status);

                if (!Strings.isNullOrEmpty(convert)) {
                    try {
                        metricValue = new BigDecimal(convert);
                    } catch (NumberFormatException nfe) {
                        logger.error("Only numeric converter values allowed", nfe);
                    }
                } else {
                    logger.info("No converter found for metric [ Status ] with value [" + status + "]");
                }

            } else if ("AvailabilityStatus".equals(metricName)) {

                String convert = metric.convert(availabilityStatus);

                if (!Strings.isNullOrEmpty(convert)) {
                    try {
                        metricValue = new BigDecimal(convert);
                    } catch (NumberFormatException nfe) {
                        logger.error("Only numeric converter values allowed", nfe);
                    }
                } else {
                    logger.info("No converter found for metric [ AvailabilityStatus ] with value [" + availabilityStatus + "]");
                }

            } else {
                metricValue = metricNameandValue.get(metricName);
            }

            if (metricValue != null) {
                printMetric(metric.getPrefix() + destinationName + "|" + metricName, metricValue, metric.getType());
            } else {
                logger.debug("Ignoring metric [" + destinationName + "|" + metricName + "] as the value is null");
            }
        }
    }

    private void printMetric(String metricPath, BigDecimal value, String metricType) {
        configuration.getMetricWriter().printMetric(metricPath, value, metricType);

        if (logger.isDebugEnabled()) {
            logger.debug("Metric [" + metricType + "] metric = " + metricPath + " = " + value);
        }
    }

    private Collection<? extends Description> excludeConfigured(List<? extends Description> allElements, List<String> excludedPattern) {
        return Collections2.filter(allElements, new ExcludePatternPredicate(excludedPattern));
    }

    private Collection<? extends Description> includeConfigured(List<? extends Description> allElements, List<String> includePattern) {
        return Collections2.filter(allElements, new IncludePatternPredicate(includePattern));
    }

    private void parseTopicResult(String namespace, List<TopicDescription> topics, List<String> excludeTopics, List<Map> topicMetricsFromConfig) {

        Collection<TopicDescription> filteredTopics = null;
        if (excludeTopics != null && !excludeTopics.isEmpty()) {
            filteredTopics = (Collection<TopicDescription>) excludeConfigured(topics, excludeTopics);
        } else {
            filteredTopics = topics;
        }

        List<Metric> metrics = new ArrayList<Metric>();

        for (Map topicMetricFromConfig : topicMetricsFromConfig) {

            String metricName = (String) topicMetricFromConfig.get("name");
            String type = (String) topicMetricFromConfig.get("type");
            Map<String, String> converter = (Map<String, String>) topicMetricFromConfig.get("converter");

            Metric metric = new Metric();
            metric.setName(metricName);
            metric.setType(type);
            metric.setConverter(converter);
            metric.setPrefix(configuration.getMetricPrefix() + "|" + namespace + "|Topics|");

            metrics.add(metric);
        }

        for (TopicDescription topicInfo : filteredTopics) {

            Map<String, BigDecimal> topicMetrics = new HashMap<String, BigDecimal>();

            CountDetails countDetails = topicInfo.getCountDetails();

            Long activeMessageCount = countDetails.getActiveMessageCount();
            topicMetrics.put("ActiveMessageCount", new BigDecimal(activeMessageCount));

            Long deadLetterMessageCount = countDetails.getDeadLetterMessageCount();
            topicMetrics.put("DeadLetterMessageCount", new BigDecimal(deadLetterMessageCount));

            Long scheduledMessageCount = countDetails.getScheduledMessageCount();
            topicMetrics.put("ScheduledMessageCount", new BigDecimal(scheduledMessageCount));

            Long transferDeadLetterMessageCount = countDetails.getTransferDeadLetterMessageCount();
            topicMetrics.put("TransferDeadLetterMessageCount", new BigDecimal(transferDeadLetterMessageCount));

            Long transferMessageCount = countDetails.getTransferMessageCount();
            topicMetrics.put("TransferMessageCount", new BigDecimal(transferMessageCount));

            Long maxSizeInMegabytes = topicInfo.getMaxSizeInMegabytes();
            topicMetrics.put("MaxSizeInMegabytes", new BigDecimal(maxSizeInMegabytes));

            Long sizeInBytes = topicInfo.getSizeInBytes();
            topicMetrics.put("SizeInBytes", new BigDecimal(sizeInBytes));

            String status = topicInfo.getStatus();

            String availabilityStatus = topicInfo.getEntityAvailabilityStatus();

            printMetrics(topicInfo.getTitle(), metrics, topicMetrics, status, availabilityStatus);

        }
    }

    private static String getSASToken(String resourceUri, String keyName, String key) {
        long epoch = System.currentTimeMillis() / 1000L;
        int week = 60 * 60 * 24 * 7;
        String expiry = Long.toString(epoch + week);

        String sasToken = null;
        try {
            String stringToSign = URLEncoder.encode(resourceUri, "UTF-8") + "\n" + expiry;
            String signature = getHMAC256(key, stringToSign);
            sasToken = "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, "UTF-8") + "&sig=" +
                    URLEncoder.encode(signature, "UTF-8") + "&se=" + expiry + "&skn=" + keyName;
        } catch (UnsupportedEncodingException e) {
            logger.error("Error trying to generate SAS token", e);
            throw new RuntimeException("Error trying to generate SAS token", e);
        }

        return sasToken;
    }


    private static String getHMAC256(String key, String input) {
        Mac sha256_HMAC = null;
        String hash = null;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            hash = new String(BaseEncoding.base64().encode(sha256_HMAC.doFinal(input.getBytes("UTF-8"))));

        } catch (InvalidKeyException e) {
            logger.error("Error trying to create HmacSHA256 key", e);
            throw new RuntimeException("Error trying to create HmacSHA256 key", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error trying to create HmacSHA256 key", e);
            throw new RuntimeException("Error trying to create HmacSHA256 key", e);
        } catch (IllegalStateException e) {
            logger.error("Error trying to create HmacSHA256 key", e);
            throw new RuntimeException("Error trying to create HmacSHA256 key", e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Error trying to create HmacSHA256 key", e);
            throw new RuntimeException("Error trying to create HmacSHA256 key", e);
        }

        return hash;
    }

    private void setupHttpClient(String uri) {

        Map map = createHttpConfigMap(uri);

        //Workaround to ignore the certificate mismatch issue.
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unable to create SSL context", e);
            throw new RuntimeException("Unable to create SSL context", e);
        } catch (KeyManagementException e) {
            logger.error("Unable to create SSL context", e);
            throw new RuntimeException("Unable to create SSL context", e);
        } catch (KeyStoreException e) {
            logger.error("Unable to create SSL context", e);
            throw new RuntimeException("Unable to create SSL context", e);
        }
        HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, (X509HostnameVerifier) hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        HttpClientBuilder builder = Http4ClientBuilder.getBuilder(map);
        builder.setConnectionManager(connMgr);

        builder.setSSLSocketFactory(sslSocketFactory);

        //Keeping only Basic auth
        Registry<AuthSchemeProvider> r = RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                .build();

        builder.setDefaultAuthSchemeRegistry(r);

        httpClient = builder.build();
    }

    private Map<String, String> createHttpConfigMap(String uri) {
        Map map = new HashMap();

        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        map.put("servers", list);
        HashMap<String, String> server = new HashMap<String, String>();
        server.put("uri", uri);

        list.add(server);

        HashMap<String, String> proxyProps = new HashMap<String, String>();
        map.put("proxy", proxyProps);

        String proxyUri = (String) configuration.getConfigYml().get("proxyUri");
        String proxyUser = (String) configuration.getConfigYml().get("proxyUser");
        String proxyPassword = (String) configuration.getConfigYml().get("proxyPassword");

        proxyProps.put("uri", proxyUri);
        proxyProps.put("username", proxyUser);
        proxyProps.put("password", proxyPassword);

        return map;
    }

    private String decryptIfEncrypted(String nonEncryptedString, String encryptedString, String encryptionKey) {

        Map<String, String> map = new HashMap<String, String>();

        if (nonEncryptedString != null) {
            logger.debug("Using the provided non encrypted value");
            map.put(TaskInputArgs.PASSWORD, nonEncryptedString);
        }

        if (encryptedString != null) {
            logger.debug("Decrypting the value...");
            map.put(TaskInputArgs.PASSWORD_ENCRYPTED, encryptedString);
            map.put(TaskInputArgs.ENCRYPTION_KEY, encryptionKey);
        }

        String decryptedValue = CryptoUtil.getPassword(map);

        return decryptedValue;
    }
}