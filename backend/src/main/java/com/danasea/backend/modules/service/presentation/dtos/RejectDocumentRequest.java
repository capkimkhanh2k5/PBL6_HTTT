package com.danasea.backend.modules.service.presentation.dtos;

import jakarta.validation.constraints.NotBlank;

public record RejectDocumentRequest(
        @NotBlank(message = "rejectionReason must not be blank")
        String rejectionReason
) {}
