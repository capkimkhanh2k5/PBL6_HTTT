package com.danasea.backend.security.authentication.domain.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        UUID userId,
        String email,
        String role,
        OffsetDateTime registeredAt
) {
}
