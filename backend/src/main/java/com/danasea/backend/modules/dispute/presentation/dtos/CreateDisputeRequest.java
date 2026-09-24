package com.danasea.backend.modules.dispute.presentation.dtos;

import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateDisputeRequest(
        @NotNull(message = "{validation.dispute.sub_order.required}")
        UUID subOrderId,

        @NotNull(message = "{validation.dispute.reason.required}")
        DisputeReason reason,

        @NotBlank(message = "{validation.dispute.description.required}")
        @Size(max = 2000, message = "{validation.dispute.description.max}")
        String description,

        List<String> evidenceUrls
) {
    public CreateDisputeRequest {
        if (evidenceUrls == null) {
            evidenceUrls = List.of();
        }
    }
}
