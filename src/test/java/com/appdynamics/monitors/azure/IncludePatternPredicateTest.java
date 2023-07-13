/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.azure;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Satish Muddam
 */

public class IncludePatternPredicateTest {

    private IncludePatternPredicate includePatternPredicate;

    @Test
    public void testIncludePatternPredicateIncludeAll() {
        includePatternPredicate = new IncludePatternPredicate(Lists.<String>newArrayList(".*"));

        String name = "abc";

        Assert.assertTrue(includePatternPredicate.apply(name));

        String title = RandomStringUtils.randomAlphanumeric(5);
        name = title;

        Assert.assertTrue(includePatternPredicate.apply(name));
    }

    @Test
    public void testIncludePatternPredicateIncludePatternStart() {
        includePatternPredicate = new IncludePatternPredicate(Lists.<String>newArrayList("abc.*"));

        String name = "abc123";

        Assert.assertTrue(includePatternPredicate.apply(name));

        name = "xyz123";

        Assert.assertFalse(includePatternPredicate.apply(name));
    }

    @Test
    public void testIncludePatternPredicateIncludePatternEnd() {
        includePatternPredicate = new IncludePatternPredicate(Lists.<String>newArrayList(".*abc"));

        String name = "123abc";

        Assert.assertTrue(includePatternPredicate.apply(name));

        name = "123xyz";

        Assert.assertFalse(includePatternPredicate.apply(name));
    }
}