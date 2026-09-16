package com.danasea.backend.modules.booking.application.dtos;

import java.util.UUID;

public record CancelBookingHoldCommand(
        UUID bookingId,
        UUID customerId
) {
}
