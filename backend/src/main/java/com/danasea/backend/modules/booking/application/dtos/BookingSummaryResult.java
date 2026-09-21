package com.danasea.backend.modules.booking.application.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.BookingStatus;

public record BookingSummaryResult(
        UUID bookingId,
        UUID customerId,
        BookingStatus status,
        BigDecimal totalAmount,
        int totalItems,
        OffsetDateTime holdExpiresAt,
        OffsetDateTime createdAt
) {}
