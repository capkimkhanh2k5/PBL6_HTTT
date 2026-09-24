package com.danasea.backend.modules.order.presentation.dtos;

import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentStatus;

public record PaymentWebhookResponse(
        String eventId,
        UUID paymentId,
        PaymentStatus status,
        boolean alreadyProcessed
) {
}
