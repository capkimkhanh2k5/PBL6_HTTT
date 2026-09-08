package com.danasea.backend.modules.operation.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Settlement extends BaseDomainModel {
    private UUID vendorId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal grossAmount;
    private BigDecimal commissionAmount;
    private BigDecimal netPayableAmount;
    private SettlementStatus status;
    private OffsetDateTime generatedAt;
}
