package com.danasea.backend.modules.operation.presentation.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateReviewRequest(
        @NotNull(message = "{validation.review.rating.required}")
        @Min(value = 1, message = "{validation.review.rating.range}")
        @Max(value = 5, message = "{validation.review.rating.range}")
        Short rating,

        @NotBlank(message = "{validation.review.comment.required}")
        @Size(max = 2000, message = "{validation.review.comment.size}")
        String comment,

        @Size(max = 5, message = "{validation.review.images.size}")
        List<@NotBlank(message = "{validation.review.image.required}")
                @Size(max = 2048, message = "{validation.review.image.size}")
                @Pattern(regexp = "^https?://[^\\s]+$", message = "{validation.review.image.url}") String> images
) {}
