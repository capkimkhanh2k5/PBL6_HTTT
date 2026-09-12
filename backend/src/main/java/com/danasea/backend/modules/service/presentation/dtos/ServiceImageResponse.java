package com.danasea.backend.modules.service.presentation.dtos;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceImageResponse(
        UUID id,
        UUID serviceId,
        String url,
        short sortOrder,
        OffsetDateTime createdAt
) {
    public static ServiceImageResponse from(ServiceImageJpaEntity entity) {
        return new ServiceImageResponse(
                entity.getId(),
                entity.getServiceId(),
                entity.getUrl(),
                entity.getSortOrder(),
                entity.getCreatedAt()
        );
    }
}
