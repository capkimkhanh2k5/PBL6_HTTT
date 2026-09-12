package com.danasea.backend.security.authorization.domain.models;

import java.util.Set;
import java.util.UUID;

public record AuthorizationSubject (
    UUID userId,
    String email,
    Set<String> roles,
    Set<String> permissions
) {
    public AuthorizationSubject {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }
}
