package com.danasea.backend.modules.operation.application.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AiReviewReadApi {
    record ReviewEvidence(UUID id, UUID serviceId, int rating, String comment, OffsetDateTime createdAt) {}
    List<ReviewEvidence> findPublicByService(UUID serviceId, int limit);
    long countPublicByService(UUID serviceId);
}
