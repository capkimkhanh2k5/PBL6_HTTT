package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;

public record AdminOrderSummaryResponse(
        UUID id,
        UUID bookingId,
        UUID customerId,
        MasterOrderStatus status,
        PaymentOrderStatus paymentStatus,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        int totalItems,
        List<UUID> vendorIds,
        OffsetDateTime paymentDeadline,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
