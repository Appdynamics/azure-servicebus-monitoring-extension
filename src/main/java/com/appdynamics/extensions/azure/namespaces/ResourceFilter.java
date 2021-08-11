package com.appdynamics.extensions.azure.namespaces;
import java.util.HashSet;
import java.util.Set;

public class ResourceFilter {

    private Set<String> include = new HashSet<String>();
    private Set<ExcludeFilter> exclude = new HashSet<ExcludeFilter>();

    public Set<String> getInclude() {
        return include;
    }

    public void setInclude(Set<String> include) {
        this.include = include;
    }

    public Set<ExcludeFilter> getExclude() {
        return exclude;
    }

    public void setExclude(Set<ExcludeFilter> exclude) {
        this.exclude = exclude;
    }
}