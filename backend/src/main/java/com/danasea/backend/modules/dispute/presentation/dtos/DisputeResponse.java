package com.danasea.backend.modules.dispute.presentation.dtos;

import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DisputeResponse(
        UUID id,
        UUID orderId,
        UUID subOrderId,
        UUID customerId,
        DisputeReason reason,
        String description,
        List<String> evidenceUrls,
        DisputeStatus status,
        BigDecimal refundPercentage,
        String adminNote,
        UUID resolvedBy,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public DisputeResponse(
            UUID id,
            UUID subOrderId,
            UUID masterOrderId,
            UUID raisedBy,
            DisputeReason reason,
            String description,
            List<String> evidenceUrls,
            DisputeStatus status,
            String adminNote,
            UUID resolvedBy,
            OffsetDateTime resolvedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, masterOrderId, subOrderId, raisedBy, reason, description, evidenceUrls, status, null, adminNote, resolvedBy, resolvedAt, createdAt, updatedAt);
    }

    public UUID masterOrderId() {
        return orderId;
    }

    public UUID raisedBy() {
        return customerId;
    }

    public String resolutionNote() {
        return adminNote;
    }
}
