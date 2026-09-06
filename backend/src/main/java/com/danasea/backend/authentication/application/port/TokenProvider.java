package com.danasea.backend.authentication.application.port;

import java.util.Optional;

import com.danasea.backend.authentication.domain.model.Authentication;

public interface TokenProvider {
    
    String generateAccessToken(Authentication user);

    Optional<String> getEmail (String token);
}
