package com.danasea.backend.modules.order.domain.models;

import java.math.BigDecimal;

public record RefundEvaluationResult(
        BigDecimal refundPercentage,
        BigDecimal refundAmount,
        String policyCode
) {
    public RefundEvaluationResult(BigDecimal refundPercentage, BigDecimal refundAmount) {
        this(refundPercentage, refundAmount, tierCode(refundPercentage));
    }

    private static String tierCode(BigDecimal percentage) {
        if (percentage == null) return "REFUND_0";
        return "REFUND_" + percentage.stripTrailingZeros().toPlainString();
    }
}
