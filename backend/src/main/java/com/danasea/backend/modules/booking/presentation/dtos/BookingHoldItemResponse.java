package com.danasea.backend.modules.booking.presentation.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingHoldItemResponse(
        UUID id,
        UUID serviceId,
        UUID vendorId,
        UUID slotId,
        Integer quantity,
        LocalDate bookingDate,
        LocalTime bookingTime,
        BigDecimal price,
        BigDecimal subtotal
) {}
