package com.danasea.backend.modules.dispute.presentation.dtos;

import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateDisputeRequest(
        @NotNull(message = "SubOrder ID is required")
        UUID subOrderId,

        @NotNull(message = "Dispute reason is required")
        DisputeReason reason,

        @NotBlank(message = "Description cannot be blank")
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        List<String> evidenceUrls
) {
    public CreateDisputeRequest {
        if (evidenceUrls == null) {
            evidenceUrls = List.of();
        }
    }
}
