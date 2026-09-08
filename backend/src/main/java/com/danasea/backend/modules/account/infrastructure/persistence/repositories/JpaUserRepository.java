package com.danasea.backend.modules.account.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.account.infrastructure.persistence.entities.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    java.util.Optional<UserJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
