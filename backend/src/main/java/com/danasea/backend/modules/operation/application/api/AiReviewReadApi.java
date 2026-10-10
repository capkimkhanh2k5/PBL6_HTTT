package com.danasea.backend.modules.operation.application.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AiReviewReadApi {
    record ReviewEvidence(UUID id, UUID serviceId, int rating, String comment, OffsetDateTime createdAt) {}
    record ReviewSnapshot(long totalCount, BigDecimal averageRating, long positiveCount, long negativeCount,
                          Map<Integer, Long> ratingDistribution, List<ReviewEvidence> representatives,
                          String fingerprint, OffsetDateTime readAt) {}
    List<ReviewEvidence> findPublicByService(UUID serviceId, int limit);
    long countPublicByService(UUID serviceId);
    ReviewSnapshot snapshot(UUID serviceId, int sampleLimit);
}
