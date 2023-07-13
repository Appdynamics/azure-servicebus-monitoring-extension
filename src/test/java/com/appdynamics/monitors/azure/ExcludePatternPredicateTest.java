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

public class ExcludePatternPredicateTest {

    private ExcludePatternPredicate excludePatternPredicate;

    @Test
    public void testExcludePatternPredicateExcludeAll() {
        excludePatternPredicate = new ExcludePatternPredicate(Lists.<String>newArrayList(".*"));

        String name = "abc";

        Assert.assertFalse(excludePatternPredicate.apply(name));

        String title = RandomStringUtils.randomAlphanumeric(5);
        name = title;

        Assert.assertFalse(excludePatternPredicate.apply(name));
    }

    @Test
    public void testExcludePatternPredicateExcludePatternStart() {
        excludePatternPredicate = new ExcludePatternPredicate(Lists.<String>newArrayList("abc.*"));

        String name = "abc123";

        Assert.assertFalse(excludePatternPredicate.apply(name));

        name = "xyz123";

        Assert.assertTrue(excludePatternPredicate.apply(name));
    }

    @Test
    public void testExcludePatternPredicateExcludePatternEnd() {
        excludePatternPredicate = new ExcludePatternPredicate(Lists.<String>newArrayList(".*abc"));

        String name = "123abc";

        Assert.assertFalse(excludePatternPredicate.apply(name));

        name = "123xyz";

        Assert.assertTrue(excludePatternPredicate.apply(name));
    }


}
