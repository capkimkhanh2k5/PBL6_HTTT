package com.danasea.backend.modules.booking.domain.exceptions;

import java.util.UUID;

public class UnauthorizedBookingAccessException extends BookingDomainException {

    public UnauthorizedBookingAccessException(UUID bookingId, UUID userId) {
        super(String.format("User %s is not authorized to access booking %s", userId, bookingId));
    }

    public UnauthorizedBookingAccessException(String message) {
        super(message);
    }
}
