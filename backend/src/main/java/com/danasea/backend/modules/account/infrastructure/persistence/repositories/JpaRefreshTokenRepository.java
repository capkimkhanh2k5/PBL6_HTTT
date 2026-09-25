package com.danasea.backend.modules.account.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.danasea.backend.modules.account.infrastructure.persistence.entities.RefreshTokenJpaEntity;

@Repository
public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenJpaEntity> findForUpdateByTokenHash(String tokenHash);
    List<RefreshTokenJpaEntity> findAllByUserId(UUID userId);
    List<RefreshTokenJpaEntity> findAllByFamilyId(UUID familyId);
}
