package com.appdynamics.extensions.azure;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


public class AzureSeviceBusMonitorTest {
    @Test
    public void test() throws TaskExecutionException {
        AzureServiceBusMonitor monitor = new AzureServiceBusMonitor();
        Map<String, String> taskArgs = new HashMap<>();
        taskArgs.put("config-file", "src/test/resources/config/config.yml");
        taskArgs.put("metric-file", "src/test/resources/config/metrics.xml");
        monitor.execute(taskArgs, null);
    }
}
