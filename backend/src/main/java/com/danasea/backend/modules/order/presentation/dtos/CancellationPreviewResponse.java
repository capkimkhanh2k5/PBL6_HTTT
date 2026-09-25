package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CancellationPreviewResponse(
        UUID orderId,
        BigDecimal originalAmount,
        BigDecimal refundPercentage,
        BigDecimal refundAmount,
        String policyApplied,
        List<SubOrderCancellationPreview> items
) {
    public CancellationPreviewResponse(
            UUID orderId,
            BigDecimal originalAmount,
            BigDecimal refundPercentage,
            BigDecimal refundAmount,
            String policyApplied
    ) {
        this(orderId, originalAmount, refundPercentage, refundAmount, policyApplied, List.of());
    }
}
