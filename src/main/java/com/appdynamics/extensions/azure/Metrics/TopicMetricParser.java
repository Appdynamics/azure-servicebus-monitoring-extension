package com.appdynamics.extensions.azure.Metrics;

import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.azure.config.input.MetricConfig;
import com.appdynamics.extensions.azure.config.input.Stat;
import com.appdynamics.extensions.azure.namespaces.NamespacesInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TopicMetricParser {
    private static final org.slf4j.Logger logger = ExtensionsLoggerFactory.getLogger(QueueMetricParser.class);

    private String metricPrefix;

    private MonitorContextConfiguration configuration;

    private Stat stat;

    private MonitorContext context;

    private ObjectMapper objectMapper = new ObjectMapper();
    private MetricCollectorUtils metricCollectorUtils;
    private NamespacesInfo namespacesInfo;

    public TopicMetricParser(Stat stat, MonitorContext context, NamespacesInfo namespacesInfo, String metricPrefix, MetricCollectorUtils metricCollectorUtils) {
        this.stat = stat;
        this.context = context;
        this.metricPrefix = metricPrefix;
        this.metricCollectorUtils = metricCollectorUtils;
        this.namespacesInfo = namespacesInfo;
    }

    protected List<Metric> parseTopicResult(String queueData) {

        List<Metric> metrics = new ArrayList<Metric>();

        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(queueData)));

            NodeList feedList =  doc.getElementsByTagName("feed");

            for (int i = 0; i < feedList.getLength(); i++) {
                Element feed = (Element) feedList.item(i);
                Element entry = (Element) feed.getElementsByTagName("entry").item(0);
                Element content = (Element) entry.getElementsByTagName("content").item(0);
                Element topicDescription = (Element) content.getElementsByTagName("TopicDescription").item(0);
                String topicName = entry.getElementsByTagName("title").item(0).getTextContent();
                String prefix = "Topics|" + topicName;
                if(metricCollectorUtils.checkIncludeExcludeName(topicName, namespacesInfo.getTopicFilters())) {
                    for (MetricConfig metricConfig : stat.getMetricConfig()) {
                        if(topicDescription.getElementsByTagName(metricConfig.getAttr()).getLength() > 0) {
                            String value = topicDescription.getElementsByTagName(metricConfig.getAttr()).item(0).getTextContent();
                            metrics.add(new Metric(metricConfig.getAttr(), value, metricPrefix + "|" +  prefix + "|" + metricConfig.getAlias(), objectMapper.convertValue(metricConfig, Map.class)));

                        }
                    }
                } else {
                    logger.debug("Topic filter set, Ignoring the of the topic [{}]", topicName );
                }
            }


        } catch (Exception e) {
            logger.error("There is an error in parsing the Queue metrics" , e);
        }
        return metrics;
    }
}
