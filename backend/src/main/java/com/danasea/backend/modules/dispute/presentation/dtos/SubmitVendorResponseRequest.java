package com.danasea.backend.modules.dispute.presentation.dtos;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SubmitVendorResponseRequest(
        @NotBlank(message = "{validation.dispute.vendor_response.required}")
        String response,
        List<String> evidenceUrls
) {
}
