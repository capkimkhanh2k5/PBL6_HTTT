package com.danasea.backend.modules.booking.application.dtos;

import java.util.UUID;

public record CancelBookingCommand(
        UUID bookingId,
        UUID customerId,
        boolean isAdmin,
        String reason,
        String idempotencyKey
) {
    public CancelBookingCommand(UUID bookingId, UUID customerId, boolean isAdmin, String reason) {
        this(bookingId, customerId, isAdmin, reason,
                bookingId == null ? null : "cancel-" + bookingId);
    }
}
