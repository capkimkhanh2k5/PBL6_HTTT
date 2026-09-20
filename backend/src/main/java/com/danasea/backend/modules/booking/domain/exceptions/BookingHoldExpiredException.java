package com.danasea.backend.modules.booking.domain.exceptions;

import java.util.UUID;

public class BookingHoldExpiredException extends BookingDomainException {

    public BookingHoldExpiredException(UUID bookingId) {
        super(String.format("Booking hold for id %s has expired", bookingId));
    }

    public BookingHoldExpiredException(String message) {
        super(message);
    }
}
