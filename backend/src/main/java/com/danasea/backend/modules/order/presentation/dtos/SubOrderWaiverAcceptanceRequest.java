package com.danasea.backend.modules.order.presentation.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubOrderWaiverAcceptanceRequest(
        @NotNull(message = "{validation.accepted_flag_is_required}")
                @AssertTrue(message = "{validation.safety_waiver_must_be_accepted}")
                Boolean accepted,
        @NotNull(message = "{validation.waiver_version_is_required}") Integer version,
        @NotBlank(message = "{validation.language_is_required}") String language) {}
