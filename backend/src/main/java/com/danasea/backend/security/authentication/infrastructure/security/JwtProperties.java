package com.danasea.backend.security.authentication.infrastructure.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.jsonwebtoken.security.Keys;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties (
    String secret,
    long accessTokenMinutes
) {
    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(
            secret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
