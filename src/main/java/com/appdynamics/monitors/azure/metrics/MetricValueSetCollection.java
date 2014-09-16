package com.appdynamics.monitors.azure.metrics;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.List;

@XStreamAlias("MetricValueSetCollection")
public class MetricValueSetCollection {
    @XStreamAlias("Value")
    private List<MetricValueSet> metricValueSets;

    public List<MetricValueSet> getMetricValueSets() {
        return metricValueSets;
    }

    public void setMetricValueSets(List<MetricValueSet> metricValueSets) {
        this.metricValueSets = metricValueSets;
    }
}
