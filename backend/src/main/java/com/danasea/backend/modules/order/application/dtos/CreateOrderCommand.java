package com.danasea.backend.modules.order.application.dtos;

import java.util.UUID;

public record CreateOrderCommand(
        UUID customerId,
        UUID bookingId,
        String idempotencyKey,
        String discountCode
) {
    public CreateOrderCommand(UUID customerId, UUID bookingId, String idempotencyKey) {
        this(customerId, bookingId, idempotencyKey, null);
    }
}
