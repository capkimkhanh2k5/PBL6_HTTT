package com.danasea.backend.modules.admin.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectVendorRequest {
    @NotBlank(message = "Reason for rejection is required")
    private String reason;
}
