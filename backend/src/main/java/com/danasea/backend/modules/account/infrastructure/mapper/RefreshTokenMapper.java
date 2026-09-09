package com.danasea.backend.modules.account.infrastructure.mapper;

import org.springframework.stereotype.Component;
import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.RefreshTokenJpaEntity;

@Component
public class RefreshTokenMapper {

    public RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return RefreshToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .familyId(entity.getFamilyId())
                .replacedById(entity.getReplacedById())
                .expiresAt(entity.getExpiresAt())
                .revokedAt(entity.getRevokedAt())
                .build();
    }

    public RefreshTokenJpaEntity toEntity(RefreshToken domain) {
        if (domain == null) {
            return null;
        }

        return RefreshTokenJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tokenHash(domain.getTokenHash())
                .familyId(domain.getFamilyId())
                .replacedById(domain.getReplacedById())
                .expiresAt(domain.getExpiresAt())
                .revokedAt(domain.getRevokedAt())
                .build();
    }
}
