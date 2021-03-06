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
@XStreamAlias("entry")
public class Entry {

    @XStreamAlias("id")
    private String id;

    @XStreamAlias("title")
    private String title;

    @XStreamAlias("content")
    private Content content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }
}
