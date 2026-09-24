package com.danasea.backend.modules.order.presentation.dtos;

import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.RefundStatus;

public record RefundWebhookResponse(
        String eventId,
        UUID refundId,
        RefundStatus status,
        boolean alreadyProcessed
) {
}
