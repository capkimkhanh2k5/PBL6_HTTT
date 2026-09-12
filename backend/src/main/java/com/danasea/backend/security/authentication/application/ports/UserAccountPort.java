package com.danasea.backend.security.authentication.application.ports;

import java.util.Optional;

import com.danasea.backend.security.authentication.domain.models.Authentication;

public interface UserAccountPort {
    
    Optional<Authentication> findByEmail(String email);

    boolean existsByEmail(String email);

    Authentication save(Authentication user);
}
