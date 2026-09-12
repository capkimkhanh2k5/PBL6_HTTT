package com.danasea.backend.modules.service.presentation.dtos;

import jakarta.validation.constraints.NotBlank;

public record RejectServiceRequest(
        @NotBlank String reason
) {}
