package com.danasea.backend.modules.booking.application.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.BookingStatus;

public record BookingHoldResult(
        UUID bookingId,
        UUID customerId,
        BookingStatus status,
        BigDecimal totalAmount,
        OffsetDateTime holdExpiresAt,
        List<BookingHoldItemResult> items,
        OffsetDateTime createdAt
) {}
