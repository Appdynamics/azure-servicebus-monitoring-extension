package com.appdynamics.monitors.azure.metrics;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

@XStreamAlias("feed")
public class Feed {

    @XStreamImplicit(itemFieldName = "entry")
    private List<Entry> entries;

    @XStreamAlias("title")
    private String title;

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
