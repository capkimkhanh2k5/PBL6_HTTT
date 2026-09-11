package com.danasea.backend.modules.service.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectServiceRequest(
        @NotBlank String reason
) {}
