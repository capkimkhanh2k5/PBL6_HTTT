package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubOrderRescheduleResponse(
        UUID subOrderId,
        UUID bookingItemId,
        UUID fromSlotId,
        UUID toSlotId,
        LocalDate newBookingDate,
        LocalTime newBookingTime,
        Long rescheduleVersion,
        BigDecimal additionalAmountCharged,
        String status,
        OffsetDateTime rescheduledAt,
        String message) {}
