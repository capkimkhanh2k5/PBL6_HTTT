package com.danasea.backend.modules.operation.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FlagReviewRequest(
        @NotBlank(message = "{validation.review.reason.required}")
        @Size(max = 255, message = "{validation.review.reason.size}")
        String reason
) {}
