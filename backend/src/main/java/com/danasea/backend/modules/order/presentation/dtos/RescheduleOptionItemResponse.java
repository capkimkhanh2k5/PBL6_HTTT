package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RescheduleOptionItemResponse(
        UUID slotId,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer availableCapacity,
        boolean canAccommodate,
        String accommodateNote,
        BigDecimal referencePrice,
        BigDecimal additionalFee,
        boolean isProposedByVendor,
        UUID proposalId,
        OffsetDateTime proposalExpiresAt) {}
