package com.danasea.backend.modules.order.application.dtos;

import java.util.UUID;

public record GetOrderDetailQuery(
        UUID userId,
        UUID orderId,
        boolean isAdmin
) {}
