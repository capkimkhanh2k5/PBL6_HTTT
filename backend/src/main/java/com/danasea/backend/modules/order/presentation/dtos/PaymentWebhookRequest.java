package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentStatus;

public record PaymentWebhookRequest(
        String eventId,
        UUID paymentId,
        String providerTransactionId,
        PaymentStatus status,
        BigDecimal amount
) {
}
