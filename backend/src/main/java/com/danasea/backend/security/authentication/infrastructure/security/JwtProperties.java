package com.danasea.backend.security.authentication.infrastructure.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import io.jsonwebtoken.security.Keys;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties (
    @NotBlank String secret,
    @Positive long accessTokenMinutes,
    @Positive long refreshTokenDays
) {
    public JwtProperties {
        if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("The JWT signing secret must contain at least 256 bits.");
        }
    }

    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(
            secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public long refreshTokenMaxAgeSeconds() {
        return java.time.Duration.ofDays(refreshTokenDays).toSeconds();
    }
}
