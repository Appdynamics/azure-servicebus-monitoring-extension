/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.azure;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import java.util.List;

/**
 * @author Satish Muddam
 */
public class ExcludePatternPredicate implements Predicate<String> {

    private List<String> excludePatterns;
    private Predicate<CharSequence> patternPredicate;

    public ExcludePatternPredicate(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
        build();
    }

    private void build() {
        if (excludePatterns != null && !excludePatterns.isEmpty()) {
            for (String pattern : excludePatterns) {
                Predicate<CharSequence> charSequencePredicate = Predicates.containsPattern(pattern);
                if (patternPredicate == null) {
                    patternPredicate = charSequencePredicate;
                } else {
                    patternPredicate = Predicates.or(patternPredicate, charSequencePredicate);
                }
            }
        }

        if (patternPredicate != null) {
            patternPredicate = Predicates.not(patternPredicate);
        }

    }

    public boolean apply(String info) {

        return patternPredicate.apply(info);

    }
}