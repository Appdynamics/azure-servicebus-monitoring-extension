package com.appdynamics.monitors.azure.pojo;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Satish Muddam
 */
@XStreamAlias("QueueDescription")
public class QueueDescription extends Description {

    @XStreamAlias("MaxSizeInMegabytes")
    private long maxSizeInMegabytes;
    @XStreamAlias("MaxDeliveryCount")
    private long maxDeliveryCount;
    @XStreamAlias("SizeInBytes")
    private long SizeInBytes;
    @XStreamAlias("MessageCount")
    private long MessageCount;
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

    public long getMaxDeliveryCount() {
        return maxDeliveryCount;
    }

    public void setMaxDeliveryCount(long maxDeliveryCount) {
        this.maxDeliveryCount = maxDeliveryCount;
    }

    public long getSizeInBytes() {
        return SizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        SizeInBytes = sizeInBytes;
    }

    public long getMessageCount() {
        return MessageCount;
    }

    public void setMessageCount(long messageCount) {
        MessageCount = messageCount;
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
