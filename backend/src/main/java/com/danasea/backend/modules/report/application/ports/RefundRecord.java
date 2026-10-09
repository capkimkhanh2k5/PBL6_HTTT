package com.danasea.backend.modules.report.application.ports;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Projection record chứa thông tin Refund phục vụ thống kê báo cáo. */
public record RefundRecord(
        UUID id,
        UUID subOrderId,
        UUID paymentId,
        BigDecimal amount,
        BigDecimal refundPercentage,
        RefundReason reason,
        RefundStatus status,
        OffsetDateTime processedAt,
        OffsetDateTime createdAt) {}
