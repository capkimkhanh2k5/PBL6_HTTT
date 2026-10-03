package com.danasea.backend.modules.order.application.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CancellationPreviewResult(
        UUID orderId,
        BigDecimal originalAmount,
        BigDecimal refundPercentage,
        BigDecimal refundAmount,
        String policyApplied,
        List<SubOrderCancellationPreviewResult> items
) {
    public record SubOrderCancellationPreviewResult(
            UUID subOrderId,
            UUID serviceId,
            UUID slotId,
            LocalDateTime departureTime,
            BigDecimal originalAmount,
            BigDecimal refundPercentage,
            BigDecimal refundAmount,
            String policyApplied
    ) {}
}
