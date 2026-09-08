package com.danasea.backend.modules.order.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiscountRedemption extends BaseDomainModel {
    private UUID discountCodeId;
    private UUID masterOrderId;
    private BigDecimal amountDeducted;
}
