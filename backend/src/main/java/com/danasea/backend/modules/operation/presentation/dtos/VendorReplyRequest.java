package com.danasea.backend.modules.operation.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorReplyRequest(
        @NotBlank(message = "Reply cannot be blank")
        @Size(max = 2000, message = "Reply cannot exceed 2000 characters")
        String reply
) {}
