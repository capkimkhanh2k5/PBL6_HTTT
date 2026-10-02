package com.danasea.backend.modules.order.application.dtos;

import java.util.UUID;

public record CreateOrderCommand(
        UUID customerId,
        UUID bookingId,
        String idempotencyKey
) {}
