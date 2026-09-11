package com.danasea.backend.modules.service.infrastructure.persistence.mappers;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;

@Component
public class ServiceImageMapper {

    public ServiceImage toDomain(ServiceImageJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        ServiceImage domain = new ServiceImage();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setServiceId(entity.getServiceId());
        domain.setUrl(entity.getUrl());
        domain.setSortOrder(entity.getSortOrder());
        return domain;
    }

    public ServiceImageJpaEntity toEntity(ServiceImage domain) {
        if (domain == null) {
            return null;
        }
        ServiceImageJpaEntity entity = new ServiceImageJpaEntity();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setServiceId(domain.getServiceId());
        entity.setUrl(domain.getUrl());
        entity.setSortOrder(domain.getSortOrder() != null ? domain.getSortOrder() : (short) 0);
        return entity;
    }

    public List<ServiceImage> toDomainList(List<ServiceImageJpaEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(this::toDomain)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<ServiceImageJpaEntity> toEntityList(List<ServiceImage> domains) {
        if (domains == null) {
            return Collections.emptyList();
        }
        return domains.stream()
                .map(this::toEntity)
                .filter(Objects::nonNull)
                .toList();
    }
}
