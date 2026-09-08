package com.danasea.backend.modules.operation.infrastructure.persistence.entities;

import com.danasea.backend.modules.operation.domain.models.PayoutRequestStatus;
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
@Table(name = "payout_requests")
public class PayoutRequestJpaEntity extends BaseJpaEntity {

    private UUID vendorId;

    private UUID settlementId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PayoutRequestStatus status;

    private UUID processedBy;

    private OffsetDateTime processedAt;

}
