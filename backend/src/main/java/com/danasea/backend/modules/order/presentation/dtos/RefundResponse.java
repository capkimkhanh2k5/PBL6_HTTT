package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;

public record RefundResponse(
        UUID id,
        UUID subOrderId,
        BigDecimal amount,
        BigDecimal percentage,
        RefundReason reason,
        RefundStatus status,
        OffsetDateTime createdAt
) {
}
