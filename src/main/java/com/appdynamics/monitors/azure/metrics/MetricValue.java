package com.appdynamics.monitors.azure.metrics;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("MetricValue")
public class MetricValue {
    @XStreamAlias("Timestamp")
    private String timestamp;
    @XStreamAlias("Average")
    private String average;
    @XStreamAlias("Minimum")
    private String minimum;
    @XStreamAlias("Maximum")
    private String maximum;
    @XStreamAlias("Total")
    private String total;
    @XStreamAlias("Count")
    private String count;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAverage() {
        return average;
    }

    public void setAverage(String average) {
        this.average = average;
    }

    public String getMinimum() {
        return minimum;
    }

    public void setMinimum(String minimum) {
        this.minimum = minimum;
    }

    public String getMaximum() {
        return maximum;
    }

    public void setMaximum(String maximum) {
        this.maximum = maximum;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }
}
