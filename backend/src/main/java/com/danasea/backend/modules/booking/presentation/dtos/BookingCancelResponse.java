package com.danasea.backend.modules.booking.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.BookingStatus;

public record BookingCancelResponse(
        UUID bookingId,
        BookingStatus status,
        OffsetDateTime cancelledAt,
        OffsetDateTime earliestServiceTime,
        boolean refundEligible,
        int refundPercentage,
        BigDecimal refundAmount,
        String cancellationReason,
        String message
) {}
