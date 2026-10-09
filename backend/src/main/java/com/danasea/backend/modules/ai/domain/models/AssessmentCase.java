package com.danasea.backend.modules.ai.domain.models;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AssessmentCase(UUID id, String kind, UUID sourceId, UUID requestedBy, String status,
                             Map<String, Object> evidence, Map<String, Object> decisions,
                             UUID resolvedBy, String resolutionNote, OffsetDateTime createdAt) {}
