package com.danasea.backend.modules.order.domain.ports;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;

public record PaymentIntentResult(
        UUID paymentId,
        UUID orderId,
        PaymentProvider provider,
        BigDecimal amount,
        String paymentUrl,
        String qrCodeUrl,
        OffsetDateTime expiresAt
) {}
