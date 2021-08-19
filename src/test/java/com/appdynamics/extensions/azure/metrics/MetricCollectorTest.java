package com.appdynamics.extensions.azure.metrics;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.azure.Metrics.MetricCollectorUtils;
import com.appdynamics.extensions.azure.Metrics.MetricsCollector;
import com.appdynamics.extensions.azure.Metrics.QueueMetricParser;
import com.appdynamics.extensions.azure.Metrics.TopicMetricParser;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.azure.config.input.Stat;
import com.appdynamics.extensions.azure.namespaces.NamespacesInfo;
import com.appdynamics.extensions.azure.namespaces.Namespaces;
import com.appdynamics.extensions.yml.YmlReader;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.http.impl.client.CloseableHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;


@RunWith(PowerMockRunner.class)
@PrepareForTest(HttpClientUtils.class)
@PowerMockIgnore("javax.net.ssl.*")
public class MetricCollectorTest {
    @Mock
    private TasksExecutionServiceProvider serviceProvider;

    @Mock
    private MetricWriteHelper metricWriter;

    @Mock
    private QueueMetricParser queueMetricParser;

    @Mock
    private TopicMetricParser topicMetricParser;

    @Mock
    private MetricCollectorUtils metricsCollectorUtil = new MetricCollectorUtils();

    @Mock
    private Phaser phaser;

    private Stat.Stats stat;

    private MetricsCollector queueMetricsCollectorTask;
    private MetricsCollector topicMetricsCollectorTask;

    private MonitorContextConfiguration monitorConfiguration = new MonitorContextConfiguration("AzureServiceBus", "Custom Metrics|AzureServiceBus|", new File(""), Mockito.mock(AMonitorJob.class));

    public static final Logger logger = ExtensionsLoggerFactory.getLogger(MetricCollectorTest.class);

    private Namespaces namespaces = initialiseNamespaces(YmlReader.readFromFile(new File("src/test/resources/config/config.yml")));;

    private Map<String, String> expectedValueMap = new HashMap<String, String>();

    private List<Metric> metrics = new ArrayList<Metric>();


    @Before
    public void before(){
        monitorConfiguration.setConfigYml("src/test/resources/config/config.yml");
        monitorConfiguration.setMetricXml("src/test/resources/config/metrics.xml", Stat.Stats.class);

        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);

        stat = (Stat.Stats) monitorConfiguration.getMetricsXml();

        queueMetricsCollectorTask = Mockito.spy(new MetricsCollector(stat.getStats()[0],monitorConfiguration.getContext(), namespaces.getNamespaces()[0], metricWriter,
                phaser, "","xxx" ,true ));

        queueMetricsCollectorTask.setMetricsCollectorUtil(metricsCollectorUtil);

        topicMetricsCollectorTask = Mockito.spy(new MetricsCollector(stat.getStats()[1],monitorConfiguration.getContext(), namespaces.getNamespaces()[0], metricWriter,
                phaser, "","xxx" ,false ));

        topicMetricsCollectorTask.setMetricsCollectorUtil(metricsCollectorUtil);

        PowerMockito.mockStatic(HttpClientUtils.class);

        PowerMockito.when(HttpClientUtils.getResponse(any(CloseableHttpClient.class), anyString(), any(HttpClientUtils.ResponseConverter.class), any(HashMap.class) )).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        ObjectMapper mapper = new ObjectMapper();
                        String url = (String) invocationOnMock.getArguments()[1];
                        String var="ArrayNode";
                        String file = null;
                        if (url.contains("/Queues")) {
                            file = "src/test/resources/QueueResponse.xml";
                        } else if (url.contains("/Topics")) {
                            file = "src/test/resources/TopicResponse.xml";
                        }
                        logger.info("Returning the mocked data for the api " + file);

                        return new String(
                                Files.readAllBytes(new File(file).toPath()), StandardCharsets.UTF_8);

                    }
                });
    }


    private void initExpectedQueueMetrics() {
        expectedValueMap.put("|Queues|test1|Status","Active");
        expectedValueMap.put("|Queues|test1|MaxDeliveryCount","10");
        expectedValueMap.put("|Queues|test1|SizeInBytes","574");
        expectedValueMap.put("|Queues|test1|MessageCount","3");
        expectedValueMap.put("|Queues|test1|MaxSizeInMegabytes","16384");
    }

    private void initExpectedTopicMetrics() {
        expectedValueMap.put("|Topics|topic1|status","Active");
        expectedValueMap.put("|Topics|topic1|SizeInBytes","176");
        expectedValueMap.put("|Topics|topic1|MaxSizeInMegabytes","16384");
    }

    private void validateMetrics(MetricsCollector metricsCollectorTask){
        for(Metric metric: metricsCollectorTask.getMetrics()) {

            String actualValue = metric.getMetricValue();
            String metricName = metric.getMetricPath();
            if (expectedValueMap.containsKey(metricName)) {
                String expectedValue = expectedValueMap.get(metricName);
                Assert.assertEquals("The value of the metric " + metricName + " failed", expectedValue, actualValue);
                expectedValueMap.remove(metricName);
            } else {
                System.out.println("\"" + metricName + "\",\"" + actualValue + "\"");
                Assert.fail("Unknown Metric " + metricName);
            }
        }
    }

    @Test
    public void test() throws TaskExecutionException {
        expectedValueMap = new HashMap<String, String>();
        initExpectedTopicMetrics();
        initExpectedQueueMetrics();

        queueMetricsCollectorTask.run();
        topicMetricsCollectorTask.run();
        validateMetrics(queueMetricsCollectorTask);
        validateMetrics(topicMetricsCollectorTask);
        Assert.assertTrue("The expected values were not send. The missing values are " + expectedValueMap
                , expectedValueMap.isEmpty());
    }




    private Namespaces initialiseNamespaces(Map<String, ?> configYml) {
        Namespaces namespacesObj = new Namespaces();

        List<Map<String,?>> namespaces = (List<Map<String, ?>>) configYml.get("servers");
        if(namespaces!=null && namespaces.size()>0) {
            int index = 0;
            NamespacesInfo[] instancesToSet = new NamespacesInfo[namespaces.size()];
            for (Map<String, ?> namespace : namespaces) {
                NamespacesInfo info = new NamespacesInfo();

                ObjectMapper mapper = new ObjectMapper();

                info = mapper.convertValue(namespace, NamespacesInfo.class);
                instancesToSet[index++] = info;

            }
            namespacesObj.setNamespaces(instancesToSet);
        }
        return namespacesObj;
    }



}
