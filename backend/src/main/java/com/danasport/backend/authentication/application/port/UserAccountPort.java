package com.danasport.backend.authentication.application.port;

import java.util.Optional;

import com.danasport.backend.authentication.domain.model.AuthUser;

public interface UserAccountPort {
    
    Optional<AuthUser> findByEmail(String email);

    boolean existsByEmail(String email);

    AuthUser save(AuthUser user);
}
