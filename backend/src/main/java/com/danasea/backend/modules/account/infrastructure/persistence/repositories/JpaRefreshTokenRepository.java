package com.danasea.backend.modules.account.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.account.infrastructure.persistence.entities.RefreshTokenJpaEntity;

@Repository
public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Query("select r.userId from RefreshTokenJpaEntity r where r.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenJpaEntity> findForUpdateByTokenHash(String tokenHash);
    List<RefreshTokenJpaEntity> findAllByUserId(UUID userId);
    List<RefreshTokenJpaEntity> findAllByFamilyId(UUID familyId);
}
