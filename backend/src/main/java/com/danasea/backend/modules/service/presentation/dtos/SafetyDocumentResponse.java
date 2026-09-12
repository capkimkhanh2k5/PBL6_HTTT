package com.danasea.backend.modules.service.presentation.dtos;

import com.danasea.backend.modules.service.domain.models.DocStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSafetyDocumentJpaEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SafetyDocumentResponse(
        UUID id,
        UUID serviceId,
        String fileUrl,
        DocStatus status,
        UUID reviewedBy,
        OffsetDateTime reviewedAt,
        String rejectionReason,
        OffsetDateTime createdAt
) {
    public static SafetyDocumentResponse from(ServiceSafetyDocumentJpaEntity entity) {
        return new SafetyDocumentResponse(
                entity.getId(),
                entity.getServiceId(),
                entity.getFileUrl(),
                entity.getStatus(),
                entity.getReviewedBy(),
                entity.getReviewedAt(),
                entity.getRejectionReason(),
                entity.getCreatedAt()
        );
    }
}
