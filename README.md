# AppDynamics Azure Service Bus Monitoring Extension

This extension works only with the standalone machine agent.

## Use Case

Windows Azure is an Internet-scale computing and services platform hosted in Microsoft data centers. It includes a number of features with corresponding developer services which can be used individually or together.

## Prerequisite

Extension uses [SAS authentication](https://docs.microsoft.com/en-us/azure/service-bus-messaging/service-bus-sas)

In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Java+Agent) or [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility).  For more details on downloading these products, please  visit [here](https://download.appdynamics.com/).



## Installation

1. Download and unzip the AzureServiceMonitor.zip to the "<MachineAgent_Dir>/monitors" directory
2. Edit the file config.yml as described below in Configuration Section, located in    <MachineAgent_Dir>/monitors/AzureServiceMonitor and update the Azure server(s) details.
3. All metrics to be reported are configured in metrics.xml. Users can remove entries from metrics.xml to stop the metric from reporting.
4. Restart the Machine Agent

Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.


## Configuration


example yml configuration
   ```
   # Azure Service Bus particulars

#This will create this metric in all the tiers, under this path
#metricPrefix: Custom Metrics|Azure Service Bus|

#This will create it in specific Tier/Component. Make sure to replace <COMPONENT_ID> with the appropriate one from your environment.
#To find the <COMPONENT_ID> in your environment, please follow the screenshot https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
metricPrefix: Custom Metrics|Azure Service Bus|

numberOfThreads: 6

servers:
  - namespace: "testabhimanyu"
    displayName: "Test"
    # Provide sasKeyName,sasKey or encryptedSasKeyName,encryptedSasKey
    sasKeyName: XXXXXXXXXXXXXXX
    sasKey: XxXxxxxxxxxxxxxxx
    encryptedSasKeyName:
    encryptedSasKey:
    serviceBusRootUri: ".servicebus.windows.net"
    # Provide either include or exclude configuration.
    # If include and exclude are provided, will consider only include.
    # If include and exclude are not provided, will fetch everything.
    # Define queues to include. supports regex
    queueFilters:
      #An asterisk on its own matches all possible names.
      include: []
      #exclude all queues that starts with SYSTEM or AMQ.
      exclude:
        - type: "STARTSWITH"
          values: ["SYSTEM", "AMQ"]
    topicFilters:
      include: [ "*" ]
      exclude:
        #type value: STARTSWITH, EQUALS, ENDSWITH, CONTAINS
        - type: "STARTSWITH"
          #The name of the topic name, comma separated values
          values: []
   ```


### Metrics

Please refer to metrics.xml file located at `<MachineAgentInstallationDirectory>/monitors/AzureServiceBus/` to view the metrics which this extension can report.

### Credentials Encryption

Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

### Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

### Troubleshooting
1. Please ensure the RabbitMQ Management Plugin is enabled. Please check "" section of [this page](http://www.rabbitmq.com/management.html) for more details.
2. Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) to contact the support team.

### Support Tickets
If after going through the [Troubleshooting Document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.

    1. Stop the running machine agent.
    2. Delete all existing logs under <MachineAgent>/logs.
    3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug.
        <logger name="com.singularity">
        <logger name="com.appdynamics">
    4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
    5. Attach the zipped <MachineAgent>/conf/* directory here.
    6. Attach the zipped <MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith directory here.

For any support related questions, you can also contact help@appdynamics.com.



### Contributing

Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/azure-servicebus-monitoring-extension).

### Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |3.0.0       |
|Controller Compatibility  |4.5 or Later|
|Machine Agent Version     |4.5.13+     |
|Last Update               |20/08/2021 |
