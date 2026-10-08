package com.danasea.backend.modules.account.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.account.infrastructure.persistence.entities.PasswordResetTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {
    Optional<PasswordResetTokenJpaEntity> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId);
    List<PasswordResetTokenJpaEntity> findAllByUserIdAndUsedAtIsNull(UUID userId);
}
