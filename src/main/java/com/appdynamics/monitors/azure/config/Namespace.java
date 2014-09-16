package com.appdynamics.monitors.azure.config;

import java.util.Set;

public class Namespace {

    private String namespace;
    private Set<String> queueStats;
    private Set<String> excludeQueues;
    private Set<String> topicStats;
    private Set<String> excludeTopics;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Set<String> getQueueStats() {
        return queueStats;
    }

    public void setQueueStats(Set<String> queueStats) {
        this.queueStats = queueStats;
    }

    public Set<String> getExcludeQueues() {
        return excludeQueues;
    }

    public void setExcludeQueues(Set<String> excludeQueues) {
        this.excludeQueues = excludeQueues;
    }

    public Set<String> getTopicStats() {
        return topicStats;
    }

    public void setTopicStats(Set<String> topicStats) {
        this.topicStats = topicStats;
    }

    public Set<String> getExcludeTopics() {
        return excludeTopics;
    }

    public void setExcludeTopics(Set<String> excludeTopics) {
        this.excludeTopics = excludeTopics;
    }
}
