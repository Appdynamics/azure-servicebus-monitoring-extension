# AppDynamics Azure Service Bus Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

Windows Azure is an Internet-scale computing and services platform hosted in Microsoft data centers. It includes a number of features with corresponding developer services which can be used individually or together.


##Prerequisite
Create and export management certificate to azure
For steps to create a certificate and export to Azure visit http://gauravmantri.com/2013/08/25/consuming-windows-azure-service-management-api-in-java/

##Installation

1. Run "mvn clean install"
2. Download and unzip the file 'target/AzureServiceBusMonitor.zip' to \<machineagent install dir\}/monitors
3. Open <b>monitor.xml</b> and configure the Azure arguments

<pre>
&lt;argument name="config-file" is-required="true" default-value="monitors/AzureServiceBusMonitor/config.yml" /&gt;
</pre>

<b>config-file</b> : yml file where we define the Azure Service Bus configurations<br/>

example yml configuration
   ```
   # Azure Service Bus particulars
    azure:
    subscriptionId: "{SubscriptionId}"
    keyStoreLocation: "{KeyStoreLocation}"
    keyStorePassword: "{KeyStorePath}"
    
    # Azure Service Bus Namespaces
    namespaces:
    - namespace: "{NameSpace}"
      queueStats: [size,incoming,outgoing,length,requests.total,requests.successful,requests.failed,requests.failed.internalservererror,requests.failed.serverbusy,requests.failed.other]
      excludeQueues: []
      topicStats: [size,incoming,requests.total,requests.successful,requests.failed,requests.failed.internalservererror,requests.failed.serverbusy,requests.failed.other]
      excludeTopics: []
    - namespace: "{NameSpace}"
      queueStats: [size,incoming,outgoing,length,requests.total,requests.successful,requests.failed,requests.failed.internalservererror,requests.failed.serverbusy,requests.failed.other]
      excludeQueues: []
      topicStats: [size,incoming,requests.total,requests.successful,requests.failed,requests.failed.internalservererror,requests.failed.serverbusy,requests.failed.other]
      excludeTopics: []
    
    #prefix used to show up metrics in AppDynamics
    metricPrefix: "Custom Metrics|Azure Service Bus|"
   
   ```


##Metrics
The following metrics are reported.

###Queues

| Metrics|
|---------------- |
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Failed requests|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Incoming Messages|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Internal Server Errors|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Length|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Other Errors|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Outgoing Messages|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Server Busy Errors|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Size|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Successful Requests|
|Azure Service Bus/{NameSpace}/Queues/{QueueName}/Total Requests|

###Topics
| Metric Path  |
|---------------- |
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/Failed requests|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/Incoming Messages|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/Internal Server Errors|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/Other Errors|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/Server Busy Errors|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/Size|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/Successful Requests|
|Azure Service Bus/{NameSpace}/Topics/{TopicName}/Total Requests|

#Custom Dashboard
![](https://raw.githubusercontent.com/Appdynamics/azure-servicebus-monitoring-extension/master/azure-service-bus-dashboard.png)

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere]() community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
