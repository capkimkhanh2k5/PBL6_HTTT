package com.danasea.backend.modules.booking.domain.exceptions;

public class InvalidBookingStateException extends BookingDomainException {

    public InvalidBookingStateException(String message) {
        super(message);
    }
}
