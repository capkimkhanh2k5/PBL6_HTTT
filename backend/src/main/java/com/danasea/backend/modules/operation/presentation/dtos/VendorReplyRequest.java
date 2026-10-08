package com.danasea.backend.modules.operation.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorReplyRequest(
        @NotBlank(message = "{validation.review.reply.required}")
        @Size(max = 2000, message = "{validation.review.reply.size}")
        String reply
) {}
