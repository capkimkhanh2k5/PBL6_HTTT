package com.danasea.backend.security.authentication.infrastructure.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.security.authentication.application.port.TokenProvider;
import com.danasea.backend.security.authentication.domain.model.Authentication;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProvider {

    private final JwtProperties properties;

    @Override
    public String generateAccessToken(Authentication user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.email())
                .claim("role", user.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plus(properties.accessTokenMinutes(),
                                ChronoUnit.MINUTES)))
                .signWith(properties.secretKey())
                .compact();
    }

    @Override
    public Optional<String> getEmail(String token) {
        try {
            String email = Jwts.parser()
                    .verifyWith(properties.secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

            if (email == null || email.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(email);
        } catch (JwtException
                | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public String generateRefreshToken(Authentication user) {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(properties.secretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
