package com.danasea.backend.modules.account.infrastructure.mappers;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.account.domain.models.PasswordResetToken;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.PasswordResetTokenJpaEntity;

@Component
public class PasswordResetTokenMapper {

    public PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        PasswordResetToken domain = new PasswordResetToken();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUserId());
        domain.setTokenHash(entity.getTokenHash());
        domain.setExpiresAt(entity.getExpiresAt());
        domain.setUsedAt(entity.getUsedAt());
        domain.setFailedAttempts(entity.getFailedAttempts());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public PasswordResetTokenJpaEntity toEntity(PasswordResetToken domain) {
        if (domain == null) {
            return null;
        }

        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setTokenHash(domain.getTokenHash());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setUsedAt(domain.getUsedAt());
        entity.setFailedAttempts(domain.getFailedAttempts());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
