/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.azure;

//import com.appdynamics.monitors.azure.pojo.Description;
//import com.google.common.collect.Lists;
//import org.apache.commons.lang.RandomStringUtils;
//import org.junit.Assert;
import org.junit.Test;

/**
 * @author Satish Muddam
 */

public class IncludePatternPredicateTest {

   // private IncludePatternPredicate includePatternPredicate;

    @Test
    public void testIncludePatternPredicateIncludeAll() {
//        includePatternPredicate = new IncludePatternPredicate(Lists.<String>newArrayList(".*"));
//
//        Description description = new Description();
//        description.setTitle("abc");
//
//        Assert.assertTrue(includePatternPredicate.apply(description));
//
//        String title = RandomStringUtils.randomAlphanumeric(5);
//        description.setTitle(title);
//
//        Assert.assertTrue(includePatternPredicate.apply(description));
    }

    @Test
    public void testIncludePatternPredicateIncludePatternStart() {
//        includePatternPredicate = new IncludePatternPredicate(Lists.<String>newArrayList("abc.*"));
//
//        Description description = new Description();
//        description.setTitle("abc123");
//
//        Assert.assertTrue(includePatternPredicate.apply(description));
//
//        description.setTitle("xyz123");
//
//        Assert.assertFalse(includePatternPredicate.apply(description));
    }

    @Test
    public void testIncludePatternPredicateIncludePatternEnd() {
//        includePatternPredicate = new IncludePatternPredicate(Lists.<String>newArrayList(".*abc"));
//
//        Description description = new Description();
//        description.setTitle("123abc");
//
//        Assert.assertTrue(includePatternPredicate.apply(description));
//
//        description.setTitle("123xyz");
//
//        Assert.assertFalse(includePatternPredicate.apply(description));
    }


}
