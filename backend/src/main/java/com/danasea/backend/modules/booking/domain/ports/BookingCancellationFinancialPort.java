package com.danasea.backend.modules.booking.domain.ports;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.CancellationFinancialResult;

public interface BookingCancellationFinancialPort {
    CancellationFinancialResult requestRefund(
            UUID bookingId,
            UUID requestedBy,
            String idempotencyKey,
            OffsetDateTime requestedAt);

    void cancelUnpaidOrder(UUID bookingId);
}
