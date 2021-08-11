package com.appdynamics.extensions.azure.namespaces;

public class NamespacesInfo {

    private String namespace;
    private String serviceBusRootUri;
    private String sasKey;
    private String sasKeyName;
    private String displayName;
    private String encryptedSasKeyName;
    private String encryptedSasKey;
    //private String encryptionKey;
    private ResourceFilter queueFilters;
    private ResourceFilter topicFilters;


    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String serviceBusRootUri) {
        this.namespace = serviceBusRootUri;
    }


    public String getServiceBusRootUri() {
        return serviceBusRootUri;
    }

    public void setServiceBusRootUri(String serviceBusRootUri) {
        this.serviceBusRootUri = serviceBusRootUri;
    }

    public String getSasKey() {
        return sasKey;
    }

    public void setSasKey(String sasKey) {
        this.sasKey = sasKey;
    }

    public String getEncryptedSasKey() {
        return encryptedSasKey;
    }

    public void setEncryptedSasKey(String encryptedSasKey) {
        this.encryptedSasKey = encryptedSasKey;
    }

    public String getSasKeyName() {
        return sasKeyName;
    }

    public void setSasKeyName(String sasKeyName) {
        this.sasKeyName = sasKeyName;
    }

    public String getEncryptedSasKeyName() {
        return encryptedSasKeyName;
    }

    public void setEncryptedSasKeyName(String encryptedSasKeyName) {
        this.encryptedSasKeyName = encryptedSasKeyName;
    }

    public String getDisplayName() {
        return displayName;
    }


    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ResourceFilter getQueueFilters() {
        if(queueFilters == null){
            return new ResourceFilter();
        }
        return queueFilters;
    }

    public void setQueueFilters(ResourceFilter queueFilters) {
        this.queueFilters = queueFilters;
    }


    public ResourceFilter getTopicFilters() {
        if(topicFilters == null){
            return new ResourceFilter();
        }
        return topicFilters;
    }

    public void setTopicFilters(ResourceFilter topicFilters) {
        this.topicFilters = topicFilters;
    }


}
