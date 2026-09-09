package com.danasea.backend.modules.account.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.account.infrastructure.persistence.entities.RefreshTokenJpaEntity;

@Repository
public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);
    List<RefreshTokenJpaEntity> findAllByUserId(UUID userId);
    List<RefreshTokenJpaEntity> findAllByFamilyId(UUID familyId);
}
