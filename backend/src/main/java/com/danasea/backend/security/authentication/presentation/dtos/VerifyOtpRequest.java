package com.danasea.backend.security.authentication.presentation.dtos;

import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
        String code
) {
}
