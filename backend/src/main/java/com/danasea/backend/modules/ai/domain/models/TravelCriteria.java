package com.danasea.backend.modules.ai.domain.models;

import java.util.List;

public record TravelCriteria(String budgetBasis, List<String> includedActivities, List<String> excludedActivities,
                             List<String> defaultedFields, List<String> unsupportedConstraints,
                             List<String> clarificationQuestions, int offset, boolean useSavedPreferences) {
    public TravelCriteria {
        budgetBasis = budgetBasis == null ? "TOTAL" : budgetBasis;
        if (!List.of("TOTAL", "PER_PERSON").contains(budgetBasis) || offset < 0 || offset > 10000) {
            throw new IllegalArgumentException("Invalid budget basis or catalog offset");
        }
        includedActivities = bounded(includedActivities);
        excludedActivities = bounded(excludedActivities);
        defaultedFields = bounded(defaultedFields);
        unsupportedConstraints = bounded(unsupportedConstraints);
        clarificationQuestions = bounded(clarificationQuestions);
    }

    public static TravelCriteria empty() { return new TravelCriteria("TOTAL", null, null, null, null, null, 0, false); }

    private static List<String> bounded(List<String> values) {
        if (values == null) return List.of();
        if (values.size() > 20 || values.stream().anyMatch(value -> value == null || value.length() > 200)) {
            throw new IllegalArgumentException("Criteria fields exceed supported limits");
        }
        return List.copyOf(values);
    }
}
