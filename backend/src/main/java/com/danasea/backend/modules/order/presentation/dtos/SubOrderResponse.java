package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.SubOrderStatus;

public record SubOrderResponse(
        UUID id,
        UUID vendorId,
        UUID serviceId,
        UUID slotId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotalAmount,
        SubOrderStatus status
) {
}
