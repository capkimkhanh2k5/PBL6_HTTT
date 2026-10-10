package com.danasea.backend.modules.ai.application.ports;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerPreferenceStorePort {
    record Profile(UUID ownerId, long version, boolean enabled, List<String> interests, List<String> exclusions, OffsetDateTime updatedAt) {}
    record Recommendation(UUID id, UUID ownerId, List<UUID> serviceIds, String criteriaFingerprint, OffsetDateTime expiresAt) {}
    record Feedback(UUID id, UUID recommendationId, UUID ownerId, UUID serviceId, String signal, String idempotencyKey, String requestHash, OffsetDateTime createdAt) {}
    Profile read(UUID ownerId);
    Profile replace(UUID ownerId, long expectedVersion, boolean enabled, List<String> interests, List<String> exclusions);
    Recommendation createRecommendation(UUID ownerId, List<UUID> serviceIds, String fingerprint);
    Optional<Recommendation> findRecommendation(UUID id, UUID ownerId);
    Optional<Feedback> findFeedback(UUID ownerId, String idempotencyKey);
    Feedback createFeedback(UUID ownerId, UUID recommendationId, UUID serviceId, String signal, String idempotencyKey, String requestHash);
    List<Feedback> recentFeedback(UUID ownerId, int limit);
}
