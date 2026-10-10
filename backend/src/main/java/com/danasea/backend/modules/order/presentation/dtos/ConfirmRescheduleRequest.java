package com.danasea.backend.modules.order.presentation.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmRescheduleRequest(
        @NotNull(message = "{validation.targetslotid_is_required}") UUID targetSlotId,
        UUID proposalId,
        @NotNull(message = "{validation.expectedversion_is_required}") Long expectedVersion) {}
