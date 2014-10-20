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

import java.util.List;

public class Configuration {

    private Azure azure;
    private List<Namespace> namespaces;
    private String metricPrefix;
    private int namespaceThreads;
    private int queueThreads;
    private int topicThreads;

    public Azure getAzure() {
        return azure;
    }

    public void setAzure(Azure azure) {
        this.azure = azure;
    }

    public List<Namespace> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<Namespace> namespaces) {
        this.namespaces = namespaces;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public int getNamespaceThreads() {
        return namespaceThreads;
    }

    public void setNamespaceThreads(int namespaceThreads) {
        this.namespaceThreads = namespaceThreads;
    }

    public int getQueueThreads() {
        return queueThreads;
    }

    public void setQueueThreads(int queueThreads) {
        this.queueThreads = queueThreads;
    }

    public int getTopicThreads() {
        return topicThreads;
    }

    public void setTopicThreads(int topicThreads) {
        this.topicThreads = topicThreads;
    }
}
