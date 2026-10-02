package com.danasea.backend.modules.order.application.dtos;

import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentStatus;

public record WebhookProcessResult(
        String eventId,
        UUID paymentId,
        PaymentStatus status,
        boolean alreadyProcessed
) {}
