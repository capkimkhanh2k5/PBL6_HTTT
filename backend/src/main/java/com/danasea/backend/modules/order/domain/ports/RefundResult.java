package com.danasea.backend.modules.order.domain.ports;

import java.math.BigDecimal;

public record RefundResult(
        boolean success,
        String providerRefundId,
        BigDecimal amount,
        String message,
        GatewayRefundStatus status,
        String currency) {
    public RefundResult(boolean success, String providerRefundId, BigDecimal amount, String message) {
        this(success, providerRefundId, amount, message,
                success ? GatewayRefundStatus.COMPLETED : GatewayRefundStatus.FAILED, null);
    }
}
