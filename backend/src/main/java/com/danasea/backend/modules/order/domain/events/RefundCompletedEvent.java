package com.danasea.backend.modules.order.domain.events;

import com.danasea.backend.modules.order.domain.models.RefundStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record RefundCompletedEvent(
        UUID refundId,
        UUID subOrderId,
        UUID masterOrderId,
        UUID customerId,
        BigDecimal amount,
        RefundStatus status
) {}
