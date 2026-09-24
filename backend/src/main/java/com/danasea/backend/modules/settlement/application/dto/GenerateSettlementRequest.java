package com.danasea.backend.modules.settlement.application.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record GenerateSettlementRequest(
    @NotNull(message = "vendorId is required")
    UUID vendorId,

    @NotNull(message = "periodStart is required")
    LocalDate periodStart,

    @NotNull(message = "periodEnd is required")
    LocalDate periodEnd
) {}
