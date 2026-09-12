package com.danasea.backend.modules.booking.application.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingHoldItemResult(
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
