package com.danasea.backend.modules.order.infrastructure.persistence.entities;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sub_order_reschedule_proposals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubOrderRescheduleProposalJpaEntity extends BaseJpaEntity {

    @Column(name = "sub_order_id", nullable = false)
    private UUID subOrderId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "proposed_slot_id", nullable = false)
    private UUID proposedSlotId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "reason_type", nullable = false, length = 50)
    private String reasonType; // OPERATIONAL, WEATHER

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACCEPTED, REJECTED, EXPIRED, SUPERSEDED

    @Column(name = "proposal_version", nullable = false)
    @Builder.Default
    private Long proposalVersion = 0L;

    @Column(name = "notification_sent_at")
    private OffsetDateTime notificationSentAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
}
