package com.danasea.backend.modules.settlement.domain.models;

import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubOrderCalculationContext {
    private UUID subOrderId;
    private SubOrderStatus status;
    private BigDecimal subtotalAmount;
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;
    private BigDecimal refundPercentage;
    private boolean hasActiveDispute;
    private DisputeStatus disputeStatus;
    private BigDecimal commissionRate;
}
