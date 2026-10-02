package com.danasea.backend.security.authentication.domain.models;

public record GoogleUserInfo(
        String googleId,
        String email,
        boolean emailVerified,
        String name,
        String picture
) {
}
