package com.danasea.backend.modules.order.application.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.RefundReason;

public record RequestRefundCommand(
        UUID customerId,
        UUID orderOrSubOrderId,
        RefundReason requestedReason,
        String idempotencyKey,
        LocalDateTime cancelTime
) {}
