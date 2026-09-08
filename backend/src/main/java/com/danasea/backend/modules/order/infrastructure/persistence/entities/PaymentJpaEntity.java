package com.danasea.backend.modules.order.infrastructure.persistence.entities;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "payments")
public class PaymentJpaEntity extends BaseJpaEntity {

    private UUID masterOrderId;

    @Enumerated(EnumType.STRING)
    private PaymentProvider provider;

    private String providerTransactionId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String rawWebhookPayload;

}
