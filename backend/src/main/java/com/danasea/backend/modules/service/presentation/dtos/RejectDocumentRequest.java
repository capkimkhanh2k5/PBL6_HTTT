package com.danasea.backend.modules.service.presentation.dtos;

import jakarta.validation.constraints.NotBlank;

public record RejectDocumentRequest(
        @NotBlank(message = "{validation.rejection_reason.required}")
        String rejectionReason
) {}
