package com.danasea.backend.modules.settlement.application.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record GenerateSettlementRequest(
    @NotNull(message = "{validation.settlement.vendor.required}")
    UUID vendorId,

    @NotNull(message = "{validation.settlement.period_start.required}")
    LocalDate periodStart,

    @NotNull(message = "{validation.settlement.period_end.required}")
    LocalDate periodEnd
) {}
