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
