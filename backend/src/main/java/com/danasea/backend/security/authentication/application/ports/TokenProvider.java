package com.danasea.backend.security.authentication.application.ports;

import java.util.Optional;

import com.danasea.backend.security.authentication.domain.models.Authentication;

public interface TokenProvider {
    
    String generateAccessToken(Authentication user);
    String generateRefreshToken(Authentication user);

    Optional<String> getEmail(String token);
    boolean validateToken(String token);
}
