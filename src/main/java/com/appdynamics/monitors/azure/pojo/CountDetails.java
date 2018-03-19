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
@XStreamAlias("CountDetails")
public class CountDetails {
    @XStreamAlias("d2p1:ActiveMessageCount")
    private long activeMessageCount;
    @XStreamAlias("d2p1:DeadLetterMessageCount")
    private long deadLetterMessageCount;
    @XStreamAlias("d2p1:ScheduledMessageCount")
    private long scheduledMessageCount;
    @XStreamAlias("d2p1:TransferMessageCount")
    private long transferMessageCount;
    @XStreamAlias("d2p1:TransferDeadLetterMessageCount")
    private long transferDeadLetterMessageCount;


    public long getActiveMessageCount() {
        return activeMessageCount;
    }

    public void setActiveMessageCount(long activeMessageCount) {
        this.activeMessageCount = activeMessageCount;
    }

    public long getDeadLetterMessageCount() {
        return deadLetterMessageCount;
    }

    public void setDeadLetterMessageCount(long deadLetterMessageCount) {
        this.deadLetterMessageCount = deadLetterMessageCount;
    }

    public long getScheduledMessageCount() {
        return scheduledMessageCount;
    }

    public void setScheduledMessageCount(long scheduledMessageCount) {
        this.scheduledMessageCount = scheduledMessageCount;
    }

    public long getTransferMessageCount() {
        return transferMessageCount;
    }

    public void setTransferMessageCount(long transferMessageCount) {
        this.transferMessageCount = transferMessageCount;
    }

    public long getTransferDeadLetterMessageCount() {
        return transferDeadLetterMessageCount;
    }

    public void setTransferDeadLetterMessageCount(long transferDeadLetterMessageCount) {
        this.transferDeadLetterMessageCount = transferDeadLetterMessageCount;
    }
}
