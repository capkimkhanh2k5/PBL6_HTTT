package com.danasea.backend.modules.booking.domain.exceptions;

import java.util.UUID;

public class BookingNotFoundException extends BookingDomainException {

    public BookingNotFoundException(UUID bookingId) {
        super("Booking not found with id: " + bookingId);
    }

    public BookingNotFoundException(String message) {
        super(message);
    }
}
