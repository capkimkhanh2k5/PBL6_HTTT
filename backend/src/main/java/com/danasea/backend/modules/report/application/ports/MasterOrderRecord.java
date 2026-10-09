package com.danasea.backend.modules.report.application.ports;

import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Projection record chứa thông tin MasterOrder phục vụ thống kê báo cáo. */
public record MasterOrderRecord(
        UUID id,
        UUID bookingId,
        UUID customerId,
        MasterOrderStatus status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        UUID discountCodeId,
        PaymentOrderStatus paymentStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
