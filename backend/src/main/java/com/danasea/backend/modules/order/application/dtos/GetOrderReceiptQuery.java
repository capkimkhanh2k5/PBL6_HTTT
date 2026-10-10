package com.danasea.backend.modules.order.application.dtos;

import java.util.UUID;

public record GetOrderReceiptQuery(
        UUID orderId,
        UUID userId,
        boolean isAdmin
) {}
