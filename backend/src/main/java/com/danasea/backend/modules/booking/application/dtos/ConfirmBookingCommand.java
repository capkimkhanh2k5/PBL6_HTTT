package com.danasea.backend.modules.booking.application.dtos;

import java.util.UUID;

public record ConfirmBookingCommand(
        UUID bookingId,
        UUID customerId
) {
}
