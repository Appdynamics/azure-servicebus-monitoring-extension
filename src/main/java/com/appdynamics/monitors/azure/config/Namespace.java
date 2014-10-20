/**
 * Copyright 2014 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
