# AppDynamics Azure Service Bus Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

Windows Azure is an Internet-scale computing and services platform hosted in Microsoft data centers. It includes a number of features with corresponding developer services which can be used individually or together.

##Installation

1. Run "mvn clean install"
2. Download and unzip the file 'target/AzureServiceBusMonitor.zip' to \{machineagent install dir\}/monitors
3. Open <b>monitor.xml</b> and configure the Azure arguments

<pre>
&lt;argument name="config-file" is-required="true" default-value="monitors/AzureServiceBusMonitor/config.yml" /&gt;
</pre>

<b>config-file</b> : yml file where we define the Azure Service Bus configurations<br/>

example yml configuration
   ```
   # Azure Service Bus particulars

#This will create this metric in all the tiers, under this path
#metricPrefix: Custom Metrics|Azure Service Bus|

#This will create it in specific Tier/Component. Make sure to replace <COMPONENT_ID> with the appropriate one from your environment.
#To find the <COMPONENT_ID> in your environment, please follow the screenshot https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|Azure Service Bus|

numberOfThreads: 2

azure:
  - namespace: "appdx-dev"
    # Provide sasKeyName,sasKey or encryptedSasKeyName,encryptedSasKey
    sasKeyName:
    sasKey:
    encryptedSasKeyName:
    encryptedSasKey:
    serviceBusRootUri: ".servicebus.windows.net"
    # Provide either include or exclude configuration.
    # If include and exclude are provided, will consider only include.
    # If include and exclude are not provided, will fetch everything.
    # Define queues to include. supports regex
    includeQueues: ["test.*"]
    # Define queues to exclude. supports regex
    excludeQueues: []
    # Define topics to include. supports regex
    includeTopics: []
    # Define topics to exclude. supports regex
    excludeTopics: []
  - namespace: "appdx-dev1"
    # Provide sasKeyName,sasKey or encryptedSasKeyName,encryptedSasKey
    sasKeyName:
    sasKey:
    encryptedSasKeyName:
    encryptedSasKey:
    serviceBusRootUri: ".servicebus.windows.net"
    # Provide either include or exclude configuration.
    # If include and exclude are provided, will consider only include.
    # If include and exclude are not provided, will fetch everything.
    # Define queues to include. supports regex
    includeQueues: []
    # Define queues to exclude. supports regex
    excludeQueues: []
    # Define topics to include. supports regex
    includeTopics: []
    # Define topics to exclude. supports regex
    excludeTopics: []

encryptionKey: "hello"

#Proxy server URI
proxyUri:
#Proxy server user name
proxyUser:
#Proxy server password
proxyPassword:

# type once defined can not be changed later.
# type consists is made of: aggregationType.timeRollup.clusterRollup
queueMetrics:
  - name: "ActiveMessageCount"
    type: "OBS.CUR.COL"
  - name: "DeadLetterMessageCount"
    type: "OBS.CUR.COL"
  - name: "ScheduledMessageCount"
    type: "OBS.CUR.COL"
  - name: "TransferMessageCount"
    type: "OBS.CUR.COL"
  - name: "TransferDeadLetterMessageCount"
    type: "OBS.CUR.COL"
  - name: "MaxDeliveryCount"
    type: "OBS.CUR.COL"
  - name: "MaxSizeInMegabytes"
    type: "OBS.CUR.COL"
  - name: "MessageCount"
    type: "OBS.CUR.COL"
  - name: "SizeInBytes"
    type: "OBS.CUR.COL"
  - name: "Status"
    type: "OBS.CUR.COL"
    converter:
       Active: "1"
       Disabled: "2"
       Restoring: "3"
       SendDisabled: "4"
       ReceiveDisabled: "5"
  - name: "AvailabilityStatus"
    type: "OBS.CUR.COL"
    converter:
       Unknown: "1"
       Available: "2"
       Limited: "3"
       Restoring: "4"
topicMetrics:
  - name: "ActiveMessageCount"
    type: "OBS.CUR.COL"
  - name: "DeadLetterMessageCount"
    type: "OBS.CUR.COL"
  - name: "ScheduledMessageCount"
    type: "OBS.CUR.COL"
  - name: "TransferMessageCount"
    type: "OBS.CUR.COL"
  - name: "TransferDeadLetterMessageCount"
    type: "OBS.CUR.COL"
  - name: "MaxSizeInMegabytes"
    type: "OBS.CUR.COL"
  - name: "SizeInBytes"
    type: "OBS.CUR.COL"
  - name: "Status"
    type: "OBS.CUR.COL"
    converter:
       Active: "1"
       Disabled: "2"
       Restoring: "3"
       SendDisabled: "4"
       ReceiveDisabled: "5"
  - name: "AvailabilityStatus"
    type: "OBS.CUR.COL"
    converter:
       Unknown: "1"
       Available: "2"
       Limited: "3"
       Restoring: "4"
   
   ```


##Metrics
The following metrics are reported.

###Queues

| Metrics|
|---------------- |
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/ActiveMessageCount|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/DeadLetterMessageCount|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/ScheduledMessageCount|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/TransferMessageCount|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/TransferDeadLetterMessageCount|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/MaxDeliveryCount|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/MaxSizeInMegabytes|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/MessageCount|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/SizeInBytes|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Status|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/AvailabilityStatus|

###Topics
| Metric Path  |
|---------------- |
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/ActiveMessageCount|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/DeadLetterMessageCount|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/ScheduledMessageCount|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/TransferMessageCount|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/TransferDeadLetterMessageCount|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/MaxSizeInMegabytes|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/SizeInBytes|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/Status|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/AvailabilityStatus|

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere](https://www.appdynamics.com/community/exchange/extension/windows-azure-servicebus-monitoring-extension/) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
