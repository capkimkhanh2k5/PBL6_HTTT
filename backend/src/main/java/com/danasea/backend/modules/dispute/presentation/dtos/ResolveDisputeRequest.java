package com.danasea.backend.modules.dispute.presentation.dtos;

import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ResolveDisputeRequest(
        @NotNull(message = "Resolution is required")
        DisputeStatus resolution,

        BigDecimal refundPercentage,

        String adminNote
) {}
