package com.danasea.backend.modules.order.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MasterOrder extends BaseDomainModel {
    private UUID customerId;
    private MasterOrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private UUID discountCodeId;
}
