package com.appdynamics.monitors.azure.processor;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.azure.Utility;
import com.appdynamics.monitors.azure.pojo.Feed;
import com.appdynamics.monitors.azure.processors.QueueMetricProcessor;
import com.google.common.collect.Maps;
import com.thoughtworks.xstream.XStream;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;


@RunWith(PowerMockRunner.class)
@PrepareForTest(Utility.class)
@PowerMockIgnore("javax.net.ssl.*")
public class QueueMetricProcessorTest {

    @Mock
    CloseableHttpClient httpClient;

    XStream xStream;

    @Mock
    Phaser phaser;

    @Mock
    MetricWriteHelper metricWriteHelper;

    MonitorContextConfiguration contextConfiguration;

    QueueMetricProcessor queueMetricProcessorSpyTask;

    @Before
    public void init(){
        contextConfiguration = new MonitorContextConfiguration("AzureServiceBus", "Custom Metrics|Azure Service Bus|", Mockito.mock(File.class), Mockito.mock(AMonitorJob.class));
        contextConfiguration.setConfigYml("src/test/resources/config/config.yml");
        Map<String,?> configYml = contextConfiguration.getConfigYml();
        String metricPrefix = contextConfiguration.getMetricPrefix();

        initXStream();

        Map servers = ((List<Map>)configYml.get("servers")).get(0);
        String displayName = (String) servers.get("displayName");
        metricPrefix=metricPrefix+"|"+displayName;
        String namespace = (String) servers.get("namespace");
        String serviceBusRootUri = (String) servers.get("serviceBusRootUri");
        String plainSasKeyName = (String) servers.get("sasKeyName");
        String plainSasKey = (String) servers.get("sasKey");
        String endpoint = "/$Resources/Queues?api-version=2013-07";
        List<String> includeQueue = null;
        List<String> excludeQueue = (List<String>) servers.get("excludeQueues");
        List<Map> queueMetricsFromConfig = (List<Map>) configYml.get("queueMetrics");
        queueMetricProcessorSpyTask = Mockito.spy(new QueueMetricProcessor(httpClient,metricWriteHelper,xStream,metricPrefix,displayName,namespace,serviceBusRootUri,plainSasKeyName,plainSasKey,endpoint,includeQueue,excludeQueue,queueMetricsFromConfig,phaser));
        PowerMockito.mockStatic(Utility.class);
    }

    @Test
    public void runTest(){
        Map<String,String> expectedMap = initExpectedMetricMap();
        Map<String,String> queueMetricsMap = Maps.newHashMap();
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);

        PowerMockito.when(Utility.getStringResponseFromUrl(Mockito.any(CloseableHttpClient.class),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        String endpoint = (String) invocationOnMock.getArguments()[5];
                        String path=null;
                        if(endpoint.contains("/Queues")){
                            path = "src/test/resources/QueueResponse.xml";
                        }
                        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
                    }
                }
        );

        queueMetricProcessorSpyTask.run();

        Mockito.verify(metricWriteHelper).transformAndPrintMetrics(pathCaptor.capture());

        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            queueMetricsMap.put(m.getMetricPath(),m.getMetricValue());
        }

        Assert.assertTrue(expectedMap.equals(queueMetricsMap));

    }

    public Map<String,String> initExpectedMetricMap(){
        Map<String,String> expectedMap = Maps.newHashMap();
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|Active Message Count","3");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|DeadLetter Message Count","0");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|Scheduled Message Count","0");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|Transfer Message Count","0");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|Transfer DeadLetter Message Count","0");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|Max Delivery Count","10");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|Max Size In Megabytes","16384");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|Message Count","3");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|Size In Bytes","574");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|Status","Active");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Queues|test1|Entity Availability Status","Available");
        return expectedMap;
    }

    public void initXStream(){
        xStream = new XStream();
        xStream.ignoreUnknownElements();
        xStream.processAnnotations(Feed.class);
        xStream.allowTypeHierarchy(Feed.class);
    }

}
