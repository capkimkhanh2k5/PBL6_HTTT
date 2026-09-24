package com.danasea.backend.modules.booking.domain.ports;

import java.util.UUID;

public interface BookingPaymentStatusPort {
    boolean hasSuccessfulPayment(UUID bookingId);
}
