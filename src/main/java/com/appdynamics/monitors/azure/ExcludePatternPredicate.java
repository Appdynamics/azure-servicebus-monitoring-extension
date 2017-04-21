package com.appdynamics.monitors.azure;

import com.appdynamics.monitors.azure.pojo.Description;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import java.util.List;

/**
 * @author Satish Muddam
 */
public class ExcludePatternPredicate implements Predicate<Description> {

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

    public boolean apply(Description info) {

        return patternPredicate.apply(info.getTitle());

    }
}