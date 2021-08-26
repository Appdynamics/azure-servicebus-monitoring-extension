package com.appdynamics.monitors.azure.processor;


import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.azure.Utility;
import com.appdynamics.monitors.azure.pojo.Feed;
import com.appdynamics.monitors.azure.processors.TopicMetricProcessor;
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
public class TopicMetricProcessorTest {

    @Mock
    CloseableHttpClient httpClient;

    XStream xStream;

    @Mock
    Phaser phaser;

    @Mock
    MetricWriteHelper metricWriteHelper;

    MonitorContextConfiguration contextConfiguration;

    TopicMetricProcessor topicMetricProcessorSpyTask;

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
        String endpoint = "/$Resources/Topics?api-version=2013-07";
        List<String> includeTopic = null;
        List<String> excludeTopic = (List<String>) servers.get("excludeTopics");
        List<Map> topicMetricsFromConfig = (List<Map>) configYml.get("topicMetrics");
        topicMetricProcessorSpyTask = Mockito.spy(new TopicMetricProcessor(httpClient,metricWriteHelper,xStream,metricPrefix,displayName,namespace,serviceBusRootUri,plainSasKeyName,plainSasKey,endpoint,includeTopic,excludeTopic,topicMetricsFromConfig,phaser));
        PowerMockito.mockStatic(Utility.class);
    }

    @Test
    public void runTest(){
        Map<String,String> expectedMap = initExpectedMetricMap();
        Map<String,String> topicMetricsMap = Maps.newHashMap();
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);

        PowerMockito.when(Utility.getStringResponseFromUrl(Mockito.any(CloseableHttpClient.class),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        String endpoint = (String) invocationOnMock.getArguments()[5];
                        String path=null;
                        if(endpoint.contains("/Topics")){
                            path = "src/test/resources/TopicResponse.xml";
                        }
                        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
                    }
                }
        );

        topicMetricProcessorSpyTask.run();

        Mockito.verify(metricWriteHelper).transformAndPrintMetrics(pathCaptor.capture());

        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            topicMetricsMap.put(m.getMetricPath(),m.getMetricValue());
        }

        Assert.assertTrue(expectedMap.equals(topicMetricsMap));

    }

    public Map<String,String> initExpectedMetricMap(){
        Map<String,String> expectedMap = Maps.newHashMap();
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Topics|topic1|Active Message Count","0");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Topics|topic1|DeadLetter Message Count","0");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Topics|topic1|Scheduled Message Count","0");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Topics|topic1|Transfer Message Count","0");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Topics|topic1|Transfer DeadLetter Message Count","0");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Topics|topic1|Max Size In Megabytes","16384");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Topics|topic1|Size In Bytes","176");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Topics|topic1|Status","Active");
        expectedMap.put("Custom Metrics|Azure Service Bus|Server 1|Topics|topic1|Entity Availability Status","Available");
        return expectedMap;
    }

    public void initXStream(){
        xStream = new XStream();
        xStream.ignoreUnknownElements();
        xStream.processAnnotations(Feed.class);
        xStream.allowTypeHierarchy(Feed.class);
    }

}
