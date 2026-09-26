package com.danasea.backend.modules.order.domain.ports;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BookingOrderView(
        UUID bookingId,
        UUID customerId,
        String status,
        BigDecimal totalAmount,
        OffsetDateTime holdExpiresAt,
        List<BookingItemOrderView> items
) {
    public record BookingItemOrderView(
            UUID bookingItemId,
            UUID vendorId,
            UUID serviceId,
            UUID slotId,
            Integer quantity,
            BigDecimal price
    ) {}
}
