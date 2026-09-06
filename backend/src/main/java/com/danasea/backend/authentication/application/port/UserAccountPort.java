package com.danasea.backend.authentication.application.port;

import java.util.Optional;

import com.danasea.backend.authentication.domain.model.Authentication;

public interface UserAccountPort {
    
    Optional<Authentication> findByEmail(String email);

    boolean existsByEmail(String email);

    Authentication save(Authentication user);
}
