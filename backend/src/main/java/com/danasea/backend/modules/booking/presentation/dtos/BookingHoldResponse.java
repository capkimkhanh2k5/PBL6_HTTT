package com.danasea.backend.modules.booking.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.BookingStatus;

public record BookingHoldResponse(
        UUID bookingId,
        UUID customerId,
        BookingStatus status,
        BigDecimal totalAmount,
        OffsetDateTime holdExpiresAt,
        List<BookingHoldItemResponse> items,
        OffsetDateTime createdAt
) {}
