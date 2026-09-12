package com.danasea.backend.modules.service.infrastructure.mappers;

import java.util.Collections;
import java.util.List;

import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.RecentlyViewedJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RecentlyViewedMapper {

    public RecentlyViewed toDomain(RecentlyViewedJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        RecentlyViewed domain = new RecentlyViewed();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setUserId(entity.getUserId());
        domain.setSessionId(entity.getSessionId());
        domain.setServiceId(entity.getServiceId());
        domain.setViewedAt(entity.getViewedAt());
        return domain;
    }

    public RecentlyViewedJpaEntity toEntity(RecentlyViewed domain) {
        if (domain == null) {
            return null;
        }
        RecentlyViewedJpaEntity entity = new RecentlyViewedJpaEntity();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setUserId(domain.getUserId());
        entity.setSessionId(domain.getSessionId());
        entity.setServiceId(domain.getServiceId());
        entity.setViewedAt(domain.getViewedAt());
        return entity;
    }

    public List<RecentlyViewed> toDomainList(List<RecentlyViewedJpaEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).toList();
    }
}
