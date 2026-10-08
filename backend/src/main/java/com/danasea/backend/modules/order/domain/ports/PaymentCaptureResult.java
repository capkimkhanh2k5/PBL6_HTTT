package com.danasea.backend.modules.order.domain.ports;

import java.math.BigDecimal;

public record PaymentCaptureResult(
        boolean success,
        String captureId,
        BigDecimal amount,
        String currency,
        String status,
        String message
) {}
