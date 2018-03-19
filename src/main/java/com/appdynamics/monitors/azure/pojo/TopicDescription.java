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
@XStreamAlias("TopicDescription")
public class TopicDescription extends Description {

    @XStreamAlias("MaxSizeInMegabytes")
    private long maxSizeInMegabytes;
    @XStreamAlias("SizeInBytes")
    private long SizeInBytes;
    @XStreamAlias("Status")
    private String status;
    @XStreamAlias("EntityAvailabilityStatus")
    private String entityAvailabilityStatus;
    @XStreamAlias("CountDetails")
    private CountDetails countDetails;

    public long getMaxSizeInMegabytes() {
        return maxSizeInMegabytes;
    }

    public void setMaxSizeInMegabytes(long maxSizeInMegabytes) {
        this.maxSizeInMegabytes = maxSizeInMegabytes;
    }

    public long getSizeInBytes() {
        return SizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        SizeInBytes = sizeInBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEntityAvailabilityStatus() {
        return entityAvailabilityStatus;
    }

    public void setEntityAvailabilityStatus(String entityAvailabilityStatus) {
        this.entityAvailabilityStatus = entityAvailabilityStatus;
    }

    public CountDetails getCountDetails() {
        return countDetails;
    }

    public void setCountDetails(CountDetails countDetails) {
        this.countDetails = countDetails;
    }
}
