package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;

public record PaymentIntentResponse(
        UUID paymentId,
        UUID orderId,
        PaymentProvider provider,
        BigDecimal amount,
        PaymentStatus status,
        String paymentReference,
        OffsetDateTime createdAt
) {
}
