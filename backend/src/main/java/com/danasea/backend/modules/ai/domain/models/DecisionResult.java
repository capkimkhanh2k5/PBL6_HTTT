package com.danasea.backend.modules.ai.domain.models;

import java.util.Map;

public record DecisionResult(
        boolean available, String task, String model, String revision, String rubricVersion,
        double latencyMs, Map<String, Answer> answers, String unavailableReason,
        boolean suggestionOnly) {

    public record Answer(String type, String choice, Double confidence, Double noul,
                         Double score, Map<String, Double> probabilities) {}

    public static DecisionResult unavailable(DecisionTask task, String reason) {
        return new DecisionResult(false, task.wireName(), null, null, null, 0,
                Map.of(), reason, true);
    }

    public String choice(String key, String fallback) {
        Answer answer = answers.get(key);
        return answer != null && answer.choice() != null ? answer.choice() : fallback;
    }

    public Double probability(String key) {
        Answer answer = answers.get(key);
        return answer == null ? null : answer.noul();
    }

    public Double score(String key) {
        Answer answer = answers.get(key);
        return answer == null ? null : answer.score();
    }
}
