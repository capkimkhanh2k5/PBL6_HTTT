package com.danasea.backend.modules.order.domain.exceptions;

import java.util.UUID;

public class BookingNotEligibleForOrderException extends RuntimeException {

    public BookingNotEligibleForOrderException(UUID bookingId, String reason) {
        super("Booking " + bookingId + " is not eligible for order creation: " + reason);
    }

    public BookingNotEligibleForOrderException(String message) {
        super(message);
    }
}
