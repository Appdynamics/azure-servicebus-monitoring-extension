/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.monitors.azure.pojo.Description;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import java.util.List;

/**
 * @author Satish Muddam
 */
public class IncludePatternPredicate implements Predicate<Description> {

    private List<String> includePatterns;
    private Predicate<CharSequence> patternPredicate;

    public IncludePatternPredicate(List<String> includePatterns) {
        this.includePatterns = includePatterns;
        build();
    }

    private void build() {
        if (includePatterns != null && !includePatterns.isEmpty()) {
            for (String pattern : includePatterns) {
                Predicate<CharSequence> charSequencePredicate = Predicates.containsPattern(pattern);
                if (patternPredicate == null) {
                    patternPredicate = charSequencePredicate;
                } else {
                    patternPredicate = Predicates.or(patternPredicate, charSequencePredicate);
                }
            }
        }
    }

    public boolean apply(Description info) {
        return patternPredicate.apply(info.getTitle());
    }
}