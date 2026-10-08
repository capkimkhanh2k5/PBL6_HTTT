package com.danasea.backend.modules.operation.presentation.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReviewVisibilityRequest(
        @NotNull(message = "{validation.review.visibility.required}")
        Boolean isVisible,

        @Size(max = 255, message = "{validation.review.note.size}")
        String note
) {}
