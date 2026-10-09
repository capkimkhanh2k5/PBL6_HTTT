package com.danasea.backend.modules.report.application.ports;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Projection record chứa thông tin Payment phục vụ thống kê báo cáo. */
public record PaymentRecord(
        UUID id,
        UUID masterOrderId,
        BigDecimal amount,
        PaymentStatus status,
        PaymentProvider provider,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime paidAt) {
    public PaymentRecord(
            UUID id,
            UUID masterOrderId,
            BigDecimal amount,
            PaymentStatus status,
            PaymentProvider provider,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this(id, masterOrderId, amount, status, provider, createdAt, updatedAt, updatedAt);
    }
}
