package com.danasea.backend.modules.operation.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SettlementItem extends BaseDomainModel {
    private UUID settlementId;
    private UUID subOrderId;
    private BigDecimal amount;
}
