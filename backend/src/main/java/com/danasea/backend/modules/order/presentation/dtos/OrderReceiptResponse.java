package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderReceiptResponse(
        UUID orderId,
        String orderCode,
        String receiptCode,
        UUID customerId,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String status,
        OffsetDateTime paidAt,
        String paymentProvider,
        List<OrderReceiptItemResponse> items
) {}
