package com.danasea.backend.security.authorization.domain.model;

import java.util.Set;

public record Role (
    String name,
    Set<Permission> permissions
) {
    public Role{
        permissions = Set.copyOf(permissions);
    }
}
