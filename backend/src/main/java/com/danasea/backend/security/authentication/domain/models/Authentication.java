package com.danasea.backend.security.authentication.domain.models;

import java.util.UUID;

public record Authentication(
        UUID id,
        String email,
        String passwordHash,
        String role,
        boolean enabled,
        boolean emailVerified,
        String locale,
        long sessionVersion
) {
    public Authentication(UUID id, String email, String passwordHash, String role, boolean enabled, boolean emailVerified) {
        this(id, email, passwordHash, role, enabled, emailVerified, "vi", 0);
    }

    public Authentication(UUID id, String email, String passwordHash, String role, boolean enabled,
            boolean emailVerified, String locale) {
        this(id, email, passwordHash, role, enabled, emailVerified, locale, 0);
    }
}
