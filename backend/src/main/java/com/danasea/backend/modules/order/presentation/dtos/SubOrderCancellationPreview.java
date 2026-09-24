package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubOrderCancellationPreview(
        UUID subOrderId,
        UUID serviceId,
        UUID slotId,
        LocalDateTime departureTime,
        BigDecimal originalAmount,
        BigDecimal refundPercentage,
        BigDecimal refundAmount,
        String policyApplied
) {
}
