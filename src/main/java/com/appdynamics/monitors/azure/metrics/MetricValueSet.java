package com.appdynamics.monitors.azure.metrics;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.List;

@XStreamAlias("MetricValueSet")
public class MetricValueSet {
    @XStreamAlias("Name")
    private String name;
    @XStreamAlias("Namespace")
    private String namespace;
    @XStreamAlias("DisplayName")
    private String displayName;
    @XStreamAlias("Unit")
    private String units;
    @XStreamAlias("MetricValues")
    private List<MetricValue> metricValues;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public List<MetricValue> getMetricValues() {
        return metricValues;
    }

    public void setMetricValues(List<MetricValue> metricValues) {
        this.metricValues = metricValues;
    }
}
