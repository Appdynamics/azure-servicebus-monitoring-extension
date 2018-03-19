/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.azure;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Satish Muddam
 */
public class Metric {

    private String name;
    private String prefix;
    private String type;
    private Map<String, String> converter = new HashMap<String, String>();


    private static final Logger logger = Logger.getLogger(Metric.class);


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getConverter() {
        return converter;
    }

    public void setConverter(Map<String, String> converter) {
        this.converter = converter;
    }

    public String convert(String metricValue) {
        String convertedValue = converter.get(metricValue);

        if (convertedValue == null) {
            logger.info("No converter value found for the Metric [" + name + "] with value [" + metricValue + "]");
        }

        return convertedValue;

    }
}
