package com.danasea.backend.modules.order.domain.ports;

import java.math.BigDecimal;

public record RefundResult(
        boolean success,
        String providerRefundId,
        BigDecimal amount,
        String message
) {}
