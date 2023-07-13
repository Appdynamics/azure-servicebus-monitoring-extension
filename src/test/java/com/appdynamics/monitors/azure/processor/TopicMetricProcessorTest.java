package com.appdynamics.monitors.azure.processor;


import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.azure.processors.TopicMetricProcessor;
import com.azure.monitor.query.MetricsQueryAsyncClient;
import com.azure.resourcemanager.servicebus.fluent.ServiceBusManagementClient;
import com.google.common.collect.Maps;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
@PowerMockIgnore("javax.net.ssl.*")
@Ignore
public class TopicMetricProcessorTest {

    @Mock
    Phaser phaser;

    @Mock
    MetricWriteHelper metricWriteHelper;

    @Mock
    private ServiceBusManagementClient serviceBusManagementClient;

    @Mock
    private MetricsQueryAsyncClient metricsQueryAsyncClient;

    MonitorContextConfiguration contextConfiguration;

    TopicMetricProcessor topicMetricProcessorSpyTask;

    @Before
    public void init(){
        contextConfiguration = new MonitorContextConfiguration("AzureServiceBus", "Custom Metrics|Azure Service Bus|", Mockito.mock(File.class), Mockito.mock(AMonitorJob.class));
        contextConfiguration.setConfigYml("src/test/resources/config/config.yml");
        Map<String,?> configYml = contextConfiguration.getConfigYml();
        String metricPrefix = contextConfiguration.getMetricPrefix();

        Map servers = ((List<Map>)configYml.get("servers")).get(0);
        String displayName = (String) servers.get("displayName");
        metricPrefix=metricPrefix+"|"+displayName;
        String resourceGroup = (String) servers.get("resourceGroup");
        String namespace = (String) servers.get("namespace");
        List<String> includeTopic = null;
        List<String> excludeTopic = (List<String>) servers.get("excludeTopics");
        List<Map> topicMetricsFromConfig = (List<Map>) configYml.get("topicMetrics");
        topicMetricProcessorSpyTask = Mockito.spy(new TopicMetricProcessor(serviceBusManagementClient, metricsQueryAsyncClient, metricWriteHelper,metricPrefix,displayName,resourceGroup,namespace,includeTopic,excludeTopic,topicMetricsFromConfig,phaser));
    }

    @Test
    public void runTest(){
        Map<String,String> expectedMap = initExpectedMetricMap();
        Map<String,String> topicMetricsMap = Maps.newHashMap();
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);

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
}
