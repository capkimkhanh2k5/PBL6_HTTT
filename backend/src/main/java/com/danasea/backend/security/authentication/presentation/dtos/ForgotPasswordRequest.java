package com.danasea.backend.security.authentication.presentation.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.email.invalid}")
        String email
) {}
