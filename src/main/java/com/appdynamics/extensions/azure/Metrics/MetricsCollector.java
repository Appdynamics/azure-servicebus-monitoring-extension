package com.appdynamics.extensions.azure.Metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.azure.config.input.Stat;
import com.appdynamics.extensions.azure.namespaces.NamespacesInfo;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Phaser;

public class MetricsCollector implements Runnable {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MetricsCollector.class);

    private Stat stat;

    private MonitorContext context;

    private String metricPrefix;

    private NamespacesInfo namespacesInfo;

    private MetricWriteHelper metricWriteHelper;

    private List<Metric> metrics = new ArrayList<Metric>();

    private MetricCollectorUtils metricCollectorUtils = new MetricCollectorUtils();
    private Phaser phaser;
    private String responseData;
    private Boolean queueMetricsFlag = true;
    private String sasToken;

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetricsCollectorUtil(MetricCollectorUtils metricsCollectorUtil) {
        this.metricCollectorUtils = metricsCollectorUtil;
    }

    public MetricsCollector(Stat stat, MonitorContext context, NamespacesInfo namespacesInfo, MetricWriteHelper metricWriteHelper, Phaser phaser, String metricPrefix, String sasToken, Boolean queueMetricsFlag)
    {
        logger.info("Initializing the Queue metric collector");
        this.stat = stat;
        this.context = context;
        this.namespacesInfo = namespacesInfo;
        this.metricWriteHelper = metricWriteHelper;
        this.phaser = phaser;
        this.metricPrefix = metricPrefix;
        this.queueMetricsFlag = queueMetricsFlag;
        this.sasToken = sasToken;
    }


    @Override
    public void run() {

        try {

            Map<String, String> headers = new HashMap<String, String>();;
            String endpoint = stat.getUrl();
            headers.put("Authorization", sasToken);

            final String url = "https://" + namespacesInfo.getNamespace() + namespacesInfo.getServiceBusRootUri() + endpoint;
            responseData = HttpClientUtils.getResponse(this.context.getHttpClient(), url,new HttpClientUtils.ResponseConverter<String>() {
                public String convert(HttpEntity entity) {
                    try {
                        String response = EntityUtils.toString(entity);
                        if (logger.isDebugEnabled()) {
                            logger.debug("The response of the url [{}]  is [{}]", url, response);
                        }
                        return response;
                    } catch (IOException e) {
                        logger.error("Error while converting response of url [" + url + "] to string " + entity, e);
                        return null;
                    }
                }
            }, headers);

            if(queueMetricsFlag) {

                QueueMetricParser queueParser = new QueueMetricParser(stat, context, namespacesInfo, metricPrefix, metricCollectorUtils);
                metrics.addAll(queueParser.parseQueueResult(responseData));
            } else {
                TopicMetricParser topicParser = new TopicMetricParser(stat, context, namespacesInfo, metricPrefix, metricCollectorUtils);
                metrics.addAll(topicParser.parseTopicResult(responseData));
            }

            if (metrics != null && metrics.size() > 0) {
                logger.debug("Printing Queue metrics: " + metrics.size());
                metricWriteHelper.transformAndPrintMetrics(metrics);
            }

        } catch(Exception e){
            logger.error("MetricsCollector error: " , e);
        } finally {
            phaser.arriveAndDeregister();
        }


    }
}
