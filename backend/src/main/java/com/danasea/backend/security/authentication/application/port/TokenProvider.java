package com.danasea.backend.security.authentication.application.port;

import java.util.Optional;

import com.danasea.backend.security.authentication.domain.model.Authentication;

public interface TokenProvider {
    
    String generateAccessToken(Authentication user);

    Optional<String> getEmail (String token);
}
