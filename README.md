# AppDynamics Azure Service Bus Monitoring Extension

This extension works only with the standalone machine agent.

## Use Case
Windows Azure is an Internet-scale computing and services platform hosted in Microsoft data centers. It includes a number of features with corresponding developer services which can be used individually or together.

## Prerequisites
1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met
2. Extension uses service principal based authentication. For more details please refer [here](https://learn.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal).
3. The extension needs to be able to connect to Azure services in order to collect and send metrics. To do this, you will have to establish a successful connection in between the extension and the product.

## Installation
1. Run "mvn clean install" from "AzureServiceBusMonitorRepo"
2. Unzip the contents of AzureServiceBusMonitor-\<version\>.zip file (&lt;AzureServiceBusMonitor&gt; / targets) to the "<MachineAgent_Dir>/monitors" directory
3. Edit the file config.yml as described below in Configuration Section, located in <MachineAgent_Dir>/monitors/AzureServiceBusMonitor and update the Azure server(s) details.
4. All metrics to be reported are also configured in config.yml file.
5. Restart the Machine Agent

Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.

## Configuration

### Config.yml

#### Configure metric prefix
Please follow section 2.1 of the [Document](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695) to set up metric prefix.
```
#Metric prefix used when SIM is enabled for your machine agent
#metricPrefix: "Custom Metrics|Azure Service Bus|"

#This will publish metrics to specific tier
#Instructions on how to retrieve the Component ID can be found in the Metric Prefix section of https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695
metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|Azure Service Bus|
```

#### Azure server configuration
Following are the details on how to configure Azure server
```
servers:
  - displayName: "Server 1"
    namespace: "CloudCollectorNS"
    resourceGroup: "cloud-collectors-rg"
    tenantId: ""
    subscriptionId: ""
    clientId: ""
    clientSecret: ""

    # Provide sasKeyName,sasKey or encryptedSasKeyName,encryptedSasKey
    #encryptedTenantId: ""
    #encryptedSubscriptionId: ""
    #encryptedClientId:
    #encryptedClientSecret:
    # Provide either include or exclude configuration.
    # If include and exclude are provided, will consider only include.
    # If include and exclude are not provided, will fetch everything.
    # Define queues to include. supports regex
    includeQueues: [".*"]
    # Define queues to exclude. supports regex
    excludeQueues: []
    # Define topics to include. supports regex
    includeTopics: [".*"]
    # Define topics to exclude. supports regex
    excludeTopics: []
```
Multiple servers can be configured like below
```
servers:
  - displayName: "Server 1"
    namespace: "CloudCollectorNS-1"
    resourceGroup: "cloud-collectors-rg-1"
    tenantId: ""
    subscriptionId: ""
    clientId: ""
    clientSecret: ""

    # Provide sasKeyName,sasKey or encryptedSasKeyName,encryptedSasKey
    #encryptedTenantId: ""
    #encryptedSubscriptionId: ""
    #encryptedClientId:
    #encryptedClientSecret:
    # Provide either include or exclude configuration.
    # If include and exclude are provided, will consider only include.
    # If include and exclude are not provided, will fetch everything.
    # Define queues to include. supports regex
    includeQueues: [".*"]
    # Define queues to exclude. supports regex
    excludeQueues: []
    # Define topics to include. supports regex
    includeTopics: [".*"]
    # Define topics to exclude. supports regex
    excludeTopics: []
  - displayName: "Server 2"
    namespace: "CloudCollectorNS-2"
    resourceGroup: "cloud-collectors-rg-2"
    tenantId: ""
    subscriptionId: ""
    clientId: ""
    clientSecret: ""

    # Provide sasKeyName,sasKey or encryptedSasKeyName,encryptedSasKey
    #encryptedTenantId: ""
    #encryptedSubscriptionId: ""
    #encryptedClientId:
    #encryptedClientSecret:
    # Provide either include or exclude configuration.
    # If include and exclude are provided, will consider only include.
    # If include and exclude are not provided, will fetch everything.
    # Define queues to include. supports regex
    includeQueues: [".*"]
    # Define queues to exclude. supports regex
    excludeQueues: []
    # Define topics to include. supports regex
    includeTopics: [".*"]
    # Define topics to exclude. supports regex
    excludeTopics: []
```
#### Number of threads
Always include: (one thread per server + 2 for queue and topic processor) + 1 (to run main task).

For example, if you have 2 Azure servers configured, then number of threads required are 7 ((1 + 2) for first server + (1 + 2) for second server + 1 thread to run main task)

#### Configure metric section

The metrics shown in the file are customizable. You can choose to modify metrics or remove an entire section (queueMetrics, topicMetrics etc) and they won't be reported. You can also add properties to individual metrics. The following properties can be added:
1. alias: The actual name of the metric as you would see it in the metric browser
2. multiplier: Used to transform the metric value, particularly for cases where memory is reported in bytes. 1.0 by default.
3. delta: Used to display a 'delta' or a difference between metrics that have an increasing value every minute. False by default.
4. clusterRollUpType: The cluster-rollup qualifier specifies how the Controller aggregates metric values in a tier (a cluster of nodes). The value is an enumerated type. Valid values are **INDIVIDUAL** (default) or **COLLECTIVE**.
5. aggregationType: The aggregator qualifier specifies how the Machine Agent aggregates the values reported during a one-minute period. Valid values are **AVERAGE** (default) or **SUM** or **OBSERVATION**.
6. timeRollUpType: The time-rollup qualifier specifies how the Controller rolls up the values when it converts from one-minute granularity tables to 10-minute granularity and 60-minute granularity tables over time. Valid values are **AVERAGE** (default) or **SUM** or **CURRENT**.
7. convert: Used to report a metric that is reporting text value by converting the value to its mapped integer

More details around this can be found [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Commons-Library-Metric-Transformers/ta-p/35413)


```
queueMetrics:
  - name: "ActiveMessages"
    alias: "Active Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromQueue: "false"
  - name: "DeadletteredMessages"
    alias: "DeadLetter Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromQueue: "false"
  - name: "ScheduledMessages"
    alias: "Scheduled Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromQueue: "false"
  - name: "Messages"
    alias: "Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromQueue: "false"
  - name: "Size"
    alias: "Size In Bytes"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromQueue: "false"
  - name: "IncomingRequests"
    alias: "Incoming Requests"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "SuccessfulRequests"
    alias: "Successful Requests"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "ServerErrors"
    alias: "Server Errors"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "UserErrors"
    alias: "User Errors"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "ThrottledRequests"
    alias: "Throttled Requests"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "TransferMessageCount"
    alias: "Transfer Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromQueue: "true"
  - name: "TransferDeadLetterMessageCount"
    alias: "Transfer DeadLetter Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromQueue: "true"
  - name: "MaxDeliveryCount"
    alias: "Max Delivery Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromQueue: "true"
  - name: "MaxSizeInMegabytes"
    alias: "Max Size In Megabytes"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromQueue: "true"
  - name: "Status"
    alias: "Status"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromQueue: "true"
    convert:
      Active: "1"
      Disabled: "2"
      Restoring: "3"
      SendDisabled: "4"
      ReceiveDisabled: "5"
      Creating: "6"
      Deleting: "7"
      Renaming: "8"
      Unknown: "9"

topicMetrics:
  - name: "ActiveMessages"
    alias: "Active Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "DeadletteredMessages"
    alias: "DeadLetter Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "ScheduledMessages"
    alias: "Scheduled Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "Size"
    alias: "Size In Bytes"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "IncomingRequests"
    alias: "Incoming Requests"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "SuccessfulRequests"
    alias: "Successful Requests"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "ServerErrors"
    alias: "Server Errors"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "UserErrors"
    alias: "User Errors"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "ThrottledRequests"
    alias: "Throttled Requests"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "false"
  - name: "TransferMessageCount"
    alias: "Transfer Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "true"
  - name: "TransferDeadLetterMessageCount"
    alias: "Transfer DeadLetter Message Count"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "true"
  - name: "MaxSizeInMegabytes"
    alias: "Max Size In Megabytes"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "true"
  - name: "Status"
    alias: "Status"
    multiplier: "1"
    aggregationType: "AVERAGE"
    timeRollUpType: "AVERAGE"
    clusterRollUpType: "INDIVIDUAL"
    delta: "false"
    fromTopic: "true"
    convert:
      Active: "1"
      Disabled: "2"
      Restoring: "3"
      SendDisabled: "4"
      ReceiveDisabled: "5"
      Creating: "6"
      Deleting: "7"
      Renaming: "8"
      Unknown: "9"
```

#### Yml Validation
Please copy all the contents of the config.yml file and go [here](https://jsonformatter.org/yaml-validator) . On reaching the website, paste the contents and press the “Validate YAML” button.

## Metrics
The following metrics are reported.

### Queues

| Metrics|
|---------------- |
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Active Message Count|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/DeadLetter Message Count|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Scheduled Message Count|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Transfer Message Count|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Transfer DeadLetter Message Count|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Max Delivery Count|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Max Size In Megabytes|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Message Count|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Size In Bytes|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Status|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Incoming Requests|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Successful Requests|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Server Errors|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/User Errors|
|Azure Service Bus/{ServerName}/Queues/{QueueName}/Throttled Requests|

### Topics

| Metrics|
|---------------- |
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Active Message Count|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/DeadLetter Message Count|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Scheduled Message Count|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Transfer Message Count|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Transfer DeadLetter Message Count|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Max Size In Megabytes|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Size In Bytes|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Status|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Incoming Requests|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Successful Requests|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Server Errors|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/User Errors|
|Azure Service Bus/{ServerName}/Topics/{TopicName}/Throttled Requests|

## Password encryption
Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

## Troubleshooting
Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.

## Contributing
Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/azure-servicebus-monitoring-extension).


## Version
|          Name            | Version                                                                                                  |
|--------------------------|----------------------------------------------------------------------------------------------------------|
|Extension Version         | 4.0.0                                                                                                    |
|Last Update               | 12 July 2023                                                                                             |
|ChangeList| [ChangeLog](https://github.com/Appdynamics/azure-servicebus-monitoring-extension/blob/master/CHANGES.md) |

**Note**: While extensions are maintained and supported by customers under the open-source licensing model, they interact with agents and Controllers that are subject to [AppDynamics’ maintenance and support policy](https://docs.appdynamics.com/latest/en/product-and-release-announcements/maintenance-support-for-software-versions). Some extensions have been tested with AppDynamics 4.5.13+ artifacts, but you are strongly recommended against using versions that are no longer supported.
