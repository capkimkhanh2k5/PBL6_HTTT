package com.danasea.backend.modules.operation.infrastructure.persistence.entities;

import com.danasea.backend.modules.operation.domain.models.SettlementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "settlements")
public class SettlementJpaEntity extends BaseJpaEntity {

    private UUID vendorId;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private BigDecimal grossAmount;

    private BigDecimal commissionAmount;

    private BigDecimal netPayableAmount;

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;

    private OffsetDateTime generatedAt;

}
