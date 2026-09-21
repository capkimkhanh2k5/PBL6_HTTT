package com.danasea.backend.modules.booking.application.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.BookingStatus;

public record BookingCancelResult(
        UUID bookingId,
        UUID customerId,
        BookingStatus status,
        OffsetDateTime cancelledAt,
        OffsetDateTime earliestServiceTime,
        boolean refundEligible,
        int refundPercentage,
        BigDecimal refundAmount,
        String cancellationReason,
        String message
) {}
