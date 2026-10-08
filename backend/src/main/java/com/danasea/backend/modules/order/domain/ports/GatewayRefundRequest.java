package com.danasea.backend.modules.order.domain.ports;

import java.math.BigDecimal;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;

public record GatewayRefundRequest(
        PaymentProvider provider,
        String transactionId,
        String orderId,
        String transactionDate,
        BigDecimal amount,
        String currency,
        boolean fullRefund,
        String requestId) {
}
