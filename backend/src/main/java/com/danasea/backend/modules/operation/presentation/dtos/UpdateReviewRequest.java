package com.danasea.backend.modules.operation.presentation.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateReviewRequest(
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Short rating,

        @NotBlank(message = "Comment must not be blank")
        @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
        String comment,

        List<String> images
) {}
