package com.danasea.backend.modules.ai.domain.models;

import java.util.Set;

public enum DecisionTask {
    INTENT("intent", Set.of("intent")),
    SERVICE_CATEGORY("service_category", Set.of("category")),
    REVIEW("review", Set.of("aspect", "positive")),
    MODERATION("moderation", Set.of("external_payment", "spam", "abuse")),
    RELEVANCE("relevance", Set.of("relevance")),
    COMPLAINT("complaint", Set.of("topic", "urgency")),
    RISK("risk", Set.of("needs_review"));

    private final String wireName;
    private final Set<String> answerKeys;

    DecisionTask(String wireName, Set<String> answerKeys) {
        this.wireName = wireName;
        this.answerKeys = answerKeys;
    }

    public String wireName() { return wireName; }
    public Set<String> answerKeys() { return answerKeys; }
}
