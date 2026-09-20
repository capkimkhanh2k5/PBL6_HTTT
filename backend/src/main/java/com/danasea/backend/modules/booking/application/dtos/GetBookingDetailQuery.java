package com.danasea.backend.modules.booking.application.dtos;

import java.util.UUID;

public record GetBookingDetailQuery(
        UUID bookingId,
        UUID currentUserId,
        boolean isAdmin
) {}
