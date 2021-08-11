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

public class ExcludePatternPredicateTest {

    //private ExcludePatternPredicate excludePatternPredicate;

    @Test
    public void testExcludePatternPredicateExcludeAll() {
//        excludePatternPredicate = new ExcludePatternPredicate(Lists.<String>newArrayList(".*"));
//
//        Description description = new Description();
//        description.setTitle("abc");
//
//        Assert.assertFalse(excludePatternPredicate.apply(description));
//
//        String title = RandomStringUtils.randomAlphanumeric(5);
//        description.setTitle(title);
//
//        Assert.assertFalse(excludePatternPredicate.apply(description));
    }

    @Test
    public void testExcludePatternPredicateExcludePatternStart() {
//        excludePatternPredicate = new ExcludePatternPredicate(Lists.<String>newArrayList("abc.*"));
//
//        Description description = new Description();
//        description.setTitle("abc123");
//
//        Assert.assertFalse(excludePatternPredicate.apply(description));
//
//        description.setTitle("xyz123");
//
//        Assert.assertTrue(excludePatternPredicate.apply(description));
    }

    @Test
    public void testExcludePatternPredicateExcludePatternEnd() {
//        excludePatternPredicate = new ExcludePatternPredicate(Lists.<String>newArrayList(".*abc"));
//
//        Description description = new Description();
//        description.setTitle("123abc");
//
//        Assert.assertFalse(excludePatternPredicate.apply(description));
//
//        description.setTitle("123xyz");
//
//        Assert.assertTrue(excludePatternPredicate.apply(description));
    }


}
