package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemDiscountPreviewResponse(
        UUID bookingItemId,
        UUID vendorId,
        UUID serviceId,
        BigDecimal originalSubtotal,
        boolean eligible,
        BigDecimal vendorDiscountAmount,
        BigDecimal platformDiscountAmount,
        BigDecimal discountAmount,
        BigDecimal finalSubtotal
) {}
