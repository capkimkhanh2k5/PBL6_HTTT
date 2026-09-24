package com.danasea.backend.modules.dispute.domain.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Domain Model biểu diễn một khiếu nại (Dispute) trong hệ thống.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Dispute extends BaseDomainModel {

    private UUID orderId;
    private UUID subOrderId;
    private UUID customerId;
    private DisputeReason reason;
    private String description;

    @Builder.Default
    private List<String> evidenceUrls = new ArrayList<>();

    private DisputeStatus status;
    private BigDecimal refundPercentage;
    private String adminNote;
    private OffsetDateTime resolvedAt;
    private UUID resolvedBy;

    public boolean isActive() {
        return status != null && status.isActive();
    }

    public boolean isResolved() {
        return status != null && status.isResolved();
    }

    public UUID getRaisedBy() {
        return customerId;
    }

    public void setRaisedBy(UUID raisedBy) {
        this.customerId = raisedBy;
    }

    public UUID getMasterOrderId() {
        return orderId;
    }

    public void setMasterOrderId(UUID masterOrderId) {
        this.orderId = masterOrderId;
    }

    public String getResolutionNote() {
        return adminNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.adminNote = resolutionNote;
    }
}
