package com.danasea.backend.modules.order.domain.models;

import java.math.BigDecimal;

public record RefundEvaluationResult(
        BigDecimal refundPercentage,
        BigDecimal refundAmount
) {
}
