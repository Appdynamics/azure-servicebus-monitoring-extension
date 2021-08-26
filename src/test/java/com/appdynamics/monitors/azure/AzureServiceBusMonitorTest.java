package com.appdynamics.monitors.azure;

import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AzureServiceBusMonitorTest {

    @Test
    public void test() throws TaskExecutionException {
        AzureServiceBusMonitor monitor = new AzureServiceBusMonitor();
        Map<String, String> taskArgs = new HashMap<>();
        taskArgs.put("config-file", "src/test/resources/config/config.yml");
        monitor.execute(taskArgs, null);
    }
}
