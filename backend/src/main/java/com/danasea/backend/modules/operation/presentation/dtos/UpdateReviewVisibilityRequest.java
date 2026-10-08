package com.danasea.backend.modules.operation.presentation.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReviewVisibilityRequest(
        @NotNull(message = "Visibility status is required")
        Boolean isVisible,

        @Size(max = 255, message = "Note cannot exceed 255 characters")
        String note
) {}
