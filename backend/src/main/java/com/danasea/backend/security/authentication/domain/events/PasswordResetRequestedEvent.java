package com.danasea.backend.security.authentication.domain.events;

import java.util.UUID;

public record PasswordResetRequestedEvent(
    UUID userId,
    String email,
    String otpCode,
    String locale
) {
    public PasswordResetRequestedEvent(UUID userId, String email, String otpCode) {
        this(userId, email, otpCode, "vi");
    }
}
