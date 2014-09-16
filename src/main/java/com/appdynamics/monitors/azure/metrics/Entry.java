package com.appdynamics.monitors.azure.metrics;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("entry")
public class Entry {

    @XStreamAlias("title")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}