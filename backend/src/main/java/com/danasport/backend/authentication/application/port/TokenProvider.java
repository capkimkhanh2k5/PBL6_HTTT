package com.danasport.backend.authentication.application.port;

import java.util.Optional;

import com.danasport.backend.authentication.domain.model.AuthUser;

public interface TokenProvider {
    
    String generateAccessToken(AuthUser user);

    Optional<String> getEmail (String token);
}
