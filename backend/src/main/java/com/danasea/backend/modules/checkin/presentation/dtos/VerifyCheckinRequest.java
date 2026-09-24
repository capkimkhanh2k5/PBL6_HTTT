package com.danasea.backend.modules.checkin.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record VerifyCheckinRequest(
        @NotBlank(message = "{validation.checkin.qr.required}")
        @JsonAlias({"token", "qrCode"})
        String qrToken
) {}
