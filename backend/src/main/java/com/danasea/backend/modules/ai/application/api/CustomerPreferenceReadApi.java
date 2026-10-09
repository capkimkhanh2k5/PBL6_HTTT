package com.danasea.backend.modules.ai.application.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CustomerPreferenceReadApi {
    record Signals(boolean personalizationEnabled, List<String> interests, List<String> exclusions,
                   Set<UUID> wishedServiceIds, Set<UUID> recentlyViewedServiceIds,
                   Set<UUID> positiveServiceIds, Set<UUID> negativeServiceIds) {
        public static Signals disabled() { return new Signals(false, List.of(), List.of(), Set.of(), Set.of(), Set.of(), Set.of()); }
    }
    Signals load(UUID userId);
    UUID recordRecommendation(UUID userId, List<UUID> returnedServiceIds, String criteriaFingerprint);
}
