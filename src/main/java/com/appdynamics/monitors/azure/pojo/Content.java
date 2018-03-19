/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.azure.pojo;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Satish Muddam
 */
@XStreamAlias("content")
public class Content {

    @XStreamAlias("QueueDescription")
    private QueueDescription queueDescription;

    @XStreamAlias("TopicDescription")
    private TopicDescription topicDescription;

    public QueueDescription getQueueDescription() {
        return queueDescription;
    }

    public void setQueueDescription(QueueDescription queueDescription) {
        this.queueDescription = queueDescription;
    }

    public TopicDescription getTopicDescription() {
        return topicDescription;
    }

    public void setTopicDescription(TopicDescription topicDescription) {
        this.topicDescription = topicDescription;
    }
}
