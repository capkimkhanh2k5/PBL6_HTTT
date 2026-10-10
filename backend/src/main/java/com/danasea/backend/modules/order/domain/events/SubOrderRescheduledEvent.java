package com.danasea.backend.modules.order.domain.events;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubOrderRescheduledEvent(
        UUID historyId,
        UUID subOrderId,
        UUID masterOrderId,
        UUID customerId,
        UUID vendorId,
        UUID fromSlotId,
        UUID toSlotId,
        LocalDate newBookingDate,
        LocalTime newBookingTime,
        String reasonType,
        String reason,
        OffsetDateTime rescheduledAt) {}
