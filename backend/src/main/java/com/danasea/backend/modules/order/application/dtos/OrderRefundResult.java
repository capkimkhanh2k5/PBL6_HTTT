package com.danasea.backend.modules.order.application.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;

public record OrderRefundResult(
        UUID refundId,
        UUID subOrderId,
        BigDecimal amount,
        BigDecimal refundPercentage,
        RefundReason reason,
        RefundStatus status,
        String policySummary
) {}
