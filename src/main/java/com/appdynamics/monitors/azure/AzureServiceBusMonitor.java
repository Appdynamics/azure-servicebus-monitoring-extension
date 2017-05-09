package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.appdynamics.monitors.azure.pojo.Feed;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.thoughtworks.xstream.XStream;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Satish Muddam
 */
public class AzureServiceBusMonitor extends AManagedMonitor {


    private static final String METRIC_PREFIX = "Custom Metrics|Azure Service Bus|";

    private static final Logger logger = Logger.getLogger(AzureServiceBusMonitor.class);

    private static final String CONFIG_ARG = "config-file";

    private boolean initialized;
    private MonitorConfiguration configuration;
    private XStream xStream;


    public AzureServiceBusMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }


    private static String getImplementationVersion() {
        return AzureServiceBusMonitor.class.getPackage().getImplementationTitle();
    }

    public TaskOutput execute(Map<String, String> args, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        logger.info("Starting the Azure ServiceBus Monitoring task.");

        try {
            if (!initialized) {
                initialize(args);
            }
            configuration.executeTask();

            logger.info("Finished Azure ServiceBus monitor execution");
            return new TaskOutput("Finished Azure ServiceBus monitor execution");
        } catch (Exception e) {
            logger.error("Failed to execute the Azure ServiceBus monitoring task", e);
            throw new TaskExecutionException("Failed to execute the Azure ServiceBus monitoring task" + e);
        }
    }

    private void initialize(Map<String, String> argsMap) {
        if (!initialized) {
            final String configFilePath = argsMap.get(CONFIG_ARG);

            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
            conf.setConfigYml(configFilePath);

            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.METRIC_PREFIX,
                    MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE);
            this.configuration = conf;

            initXstream();

            initialized = true;
        }
    }

    private void initXstream() {
        xStream = new XStream();
        xStream.ignoreUnknownElements();
        xStream.processAnnotations(Feed.class);
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            if (!initialized) {
                logger.info("Azure ServiceBus Monitor is still initializing");
                return;
            }

            Map<String, ?> config = configuration.getConfigYml();

            List<Map> azureConfigurations = (List<Map>) config.get("azure");

            for (Map azureConfiguration : azureConfigurations) {
                AzureServiceBusMonitoringTask task = new AzureServiceBusMonitoringTask(configuration, azureConfiguration, xStream);
                configuration.getExecutorService().execute(task);
            }
        }
    }


    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        logger.getRootLogger().addAppender(ca);

        final AzureServiceBusMonitor monitor = new AzureServiceBusMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "/Users/Muddam/AppDynamics/Code/extensions/azure-servicebus-monitoring-extension/src/main/resources/config/config.yml");

        //monitor.execute(taskArgs, null);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the task", e);
                }
            }
        }, 2, 30, TimeUnit.SECONDS);
    }
}
