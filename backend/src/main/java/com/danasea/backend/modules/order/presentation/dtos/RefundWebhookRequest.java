package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.RefundStatus;

public record RefundWebhookRequest(
        String eventId,
        UUID refundId,
        String providerRefundId,
        RefundStatus status,
        BigDecimal amount
) {
}
