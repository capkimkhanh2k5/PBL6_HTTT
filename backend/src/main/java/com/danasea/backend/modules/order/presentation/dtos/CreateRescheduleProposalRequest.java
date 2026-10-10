package com.danasea.backend.modules.order.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateRescheduleProposalRequest(
        @NotNull(message = "{validation.proposedslotid_is_required}") UUID proposedSlotId,
        @NotBlank(message = "{validation.reason_is_required}")
                @Size(max = 500, message = "{validation.reason_cannot_exceed_500_characters}")
                String reason,
        String reasonType, // "OPERATIONAL" or "WEATHER", default OPERATIONAL
        OffsetDateTime expiresAt) {}
