package com.danasea.backend.modules.checkin.infrastructure.persistence.mappers;

import com.danasea.backend.modules.checkin.domain.models.CheckinToken;
import com.danasea.backend.modules.checkin.infrastructure.persistence.entities.CheckinTokenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CheckinTokenMapper {

    public CheckinToken toDomain(CheckinTokenJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return CheckinToken.builder()
                .id(entity.getId())
                .subOrderId(entity.getSubOrderId())
                .qrTokenHash(entity.getQrTokenHash())
                .expiresAt(entity.getExpiresAt())
                .usedAt(entity.getUsedAt())
                .usedByVendorStaffId(entity.getUsedByVendorStaffId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public CheckinTokenJpaEntity toEntity(CheckinToken domain) {
        if (domain == null) {
            return null;
        }

        CheckinTokenJpaEntity entity = CheckinTokenJpaEntity.builder()
                .subOrderId(domain.getSubOrderId())
                .qrTokenHash(domain.getQrTokenHash())
                .expiresAt(domain.getExpiresAt())
                .usedAt(domain.getUsedAt())
                .usedByVendorStaffId(domain.getUsedByVendorStaffId())
                .build();

        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
