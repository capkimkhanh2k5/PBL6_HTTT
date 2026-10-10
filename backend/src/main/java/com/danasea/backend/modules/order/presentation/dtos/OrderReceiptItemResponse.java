package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderReceiptItemResponse(
        UUID subOrderId,
        UUID serviceId,
        String serviceName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {}
