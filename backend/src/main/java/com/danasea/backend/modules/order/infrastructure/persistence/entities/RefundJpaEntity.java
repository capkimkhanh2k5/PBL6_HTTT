package com.danasea.backend.modules.order.infrastructure.persistence.entities;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "refunds")
public class RefundJpaEntity extends BaseJpaEntity {

    private UUID subOrderId;

    private BigDecimal amount;

    private BigDecimal refundPercentage;

    @Enumerated(EnumType.STRING)
    private RefundReason reason;

    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    private OffsetDateTime processedAt;

    private UUID requestedBy;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "provider_refund_id", length = 150)
    private String providerRefundId;

    @Enumerated(EnumType.STRING)
    private PaymentProvider provider;

    @Column(name = "webhook_event_id", length = 150)
    private String webhookEventId;

}
