package com.danasea.backend.modules.booking.application.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.BookingStatus;

public record VendorBookingItemResult(
        UUID itemId,
        UUID bookingId,
        UUID serviceId,
        UUID vendorId,
        UUID slotId,
        Integer quantity,
        LocalDate bookingDate,
        LocalTime bookingTime,
        BigDecimal price,
        BigDecimal subtotal,
        BookingStatus bookingStatus,
        OffsetDateTime createdAt
) {}
