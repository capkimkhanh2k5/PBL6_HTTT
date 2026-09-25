package com.danasea.backend.modules.admin.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectVendorRequest {
    @NotBlank(message = "{validation.rejection_reason.required}")
    private String reason;
}
