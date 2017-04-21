package com.appdynamics.monitors.azure.pojo;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Satish Muddam
 */
@XStreamAlias("feed")
public class Feed {

    @XStreamAlias("title")
    private String title;

    @XStreamImplicit(itemFieldName = "entry")
    @XStreamAlias("entry")
    private List<Entry> entry;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Entry> getEntry() {
        return entry;
    }

    public void setEntry(List<Entry> entry) {
        this.entry = entry;
    }

    public List<QueueDescription> listQueues() {

        List<QueueDescription> queues = new ArrayList<QueueDescription>();

        if (entry != null) {
            for (Entry entryItem : entry) {
                QueueDescription queueDescription = entryItem.getContent().getQueueDescription();
                queueDescription.setTitle(entryItem.getTitle());
                queues.add(queueDescription);

            }
        }
        return queues;
    }

    public List<TopicDescription> listTopics() {

        List<TopicDescription> topics = new ArrayList<TopicDescription>();

        if (entry != null) {
            for (Entry entryItem : entry) {
                TopicDescription topicDescription = entryItem.getContent().getTopicDescription();
                topicDescription.setTitle(entryItem.getTitle());
                topics.add(topicDescription);
            }
        }
        return topics;
    }

}
