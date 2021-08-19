package com.appdynamics.extensions.azure.Metrics;

import com.appdynamics.extensions.Constants;
import com.appdynamics.extensions.azure.namespaces.IncludeFilter;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.azure.namespaces.ExcludeFilter;
import com.appdynamics.extensions.azure.namespaces.NamespacesInfo;
import com.appdynamics.extensions.azure.namespaces.ResourceFilter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MetricCollectorUtils {
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(MetricCollectorUtils.class);

    public static enum FilterType {
        STARTSWITH, EQUALS, ENDSWITH, CONTAINS, NONE;
    }

    protected Map<String,String> getUrlParametersMap(NamespacesInfo info) {
        Map<String,String> map = new HashMap<String, String>();
        map.put(Constants.HOST, info.getNamespace() + info.getServiceBusRootUri());
        map.put(Constants.PORT,   "");
        map.put(Constants.USER, "");
        map.put(Constants.PASSWORD, "");
        map.put(Constants.USE_SSL, "true");
        return map;
    }

    protected Boolean checkIncludeExcludeName(String resourceName, ResourceFilter resourceFilter) {

        Set<ExcludeFilter> excludeFilters = resourceFilter.getExclude();
        Set<String> includeFilters = resourceFilter.getInclude();
        if(included(resourceName, includeFilters)){
            return true;
        } else if (includeFilters.size() > 0) {
            return false;
        }
        if (Strings.isNullOrEmpty(resourceName))
            return true;

        for(ExcludeFilter filter : excludeFilters){
            String type = filter.getType();
        Set<String> filterValues = filter.getValues();
            if(filterValues.size() == 0)
                return true;
        switch (FilterType.valueOf(type)){
            case CONTAINS:
                for (String filterValue : filterValues) {
                    if (resourceName.contains(filterValue)) {
                        return false;
                    }
                }
                break;
            case STARTSWITH:
                for (String filterValue : filterValues) {
                    if (resourceName.startsWith(filterValue)) {
                        return false;
                    }
                }
                break;
            case NONE:
                return false;
            case EQUALS:
                for (String filterValue : filterValues) {
                    if (resourceName.equals(filterValue)) {
                        return false;
                    }
                }
                break;
            case ENDSWITH:
                for (String filterValue : filterValues) {
                    if (resourceName.endsWith(filterValue)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    private Boolean included(String resourceName, Set<String> filters) {

        for(String filter : filters){
            if(filter.equals("*")) {
                return true;
            }

            if(filter.contains("*")) {
                String name = filter.replace("*", "");
                if(resourceName.startsWith(name)) {
                    return true;
                }
            }
        }

        return false;
    }
}
