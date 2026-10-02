package com.danasea.backend.modules.order.application.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;

public record MasterOrderDetailResult(
        UUID id,
        UUID bookingId,
        UUID customerId,
        MasterOrderStatus status,
        PaymentOrderStatus paymentStatus,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        UUID discountCodeId,
        OffsetDateTime paymentDeadline,
        String idempotencyKey,
        OffsetDateTime createdAt,
        List<SubOrderDetailResult> subOrders
) {
    public static MasterOrderDetailResult fromDomain(MasterOrder order) {
        if (order == null) {
            return null;
        }
        List<SubOrderDetailResult> subOrderResults = order.getSubOrders() != null
                ? order.getSubOrders().stream().map(SubOrderDetailResult::fromDomain).toList()
                : Collections.emptyList();

        return new MasterOrderDetailResult(
                order.getId(),
                order.getBookingId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getDiscountCodeId(),
                order.getPaymentDeadline(),
                order.getIdempotencyKey(),
                order.getCreatedAt(),
                subOrderResults
        );
    }
}
