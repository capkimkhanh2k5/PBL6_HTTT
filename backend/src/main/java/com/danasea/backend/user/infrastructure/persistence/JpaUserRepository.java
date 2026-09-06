package com.danasea.backend.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository 
            extends JpaRepository<UserEntity, UUID>{
    
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
