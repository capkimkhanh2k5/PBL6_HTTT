package com.danasea.backend.security.authentication.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "{validation.otp.six_digits}")
        @Pattern(regexp = "^\\d{6}$", message = "{validation.otp.six_digits}")
        String code
) {
}
