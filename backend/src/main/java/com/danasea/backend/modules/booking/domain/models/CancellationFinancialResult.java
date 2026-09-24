package com.danasea.backend.modules.booking.domain.models;

import java.math.BigDecimal;

public record CancellationFinancialResult(
        boolean refundEligible,
        int refundPercentage,
        BigDecimal refundAmount
) {
    public static CancellationFinancialResult noRefund() {
        return new CancellationFinancialResult(false, 0, BigDecimal.ZERO);
    }
}
