package com.danasea.backend.modules.order.presentation.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RescheduleProposalResponse(
        UUID id,
        UUID subOrderId,
        UUID vendorId,
        UUID proposedSlotId,
        LocalDate proposedBookingDate,
        LocalTime proposedStartTime,
        LocalTime proposedEndTime,
        String reason,
        String reasonType,
        String status,
        Long proposalVersion,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt) {}
