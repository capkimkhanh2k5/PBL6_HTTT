package com.danasea.backend.modules.booking.application.dtos;

import java.util.UUID;

public record CancelBookingCommand(
        UUID bookingId,
        UUID customerId,
        boolean isAdmin,
        String reason
) {}
