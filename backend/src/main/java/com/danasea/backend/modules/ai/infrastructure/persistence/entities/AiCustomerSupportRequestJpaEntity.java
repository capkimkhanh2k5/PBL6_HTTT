package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_customer_support_requests", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "idempotency_key"}))
@Getter
@Setter
public class AiCustomerSupportRequestJpaEntity extends BaseJpaEntity {
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false) private UUID orderId;
    @Column(nullable = false, length = 24) private String kind;
    @Column(nullable = false, length = 1800) private String message;
    private LocalDate desiredDate;
    private UUID desiredSlotId;
    @Column(nullable = false, length = 24) private String status;
    @Column(nullable = false, length = 120) private String idempotencyKey;
    @Column(nullable = false, length = 64) private String requestHash;
    @Column(length = 1800) private String responseNote;
    private UUID handledBy;
    private OffsetDateTime handledAt;
    @Version private Long version;
}
