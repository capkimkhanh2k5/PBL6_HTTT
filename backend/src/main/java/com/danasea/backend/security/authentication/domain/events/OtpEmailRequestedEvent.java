package com.danasea.backend.security.authentication.domain.events;

import java.util.UUID;
import lombok.Value;

public record OtpEmailRequestedEvent(
    UUID userId,
    String email,
    String otpCode
) {}
