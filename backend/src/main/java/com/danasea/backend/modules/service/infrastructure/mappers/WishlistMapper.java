package com.danasea.backend.modules.service.infrastructure.mappers;

import java.util.Collections;
import java.util.List;

import com.danasea.backend.modules.service.domain.models.Wishlist;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.WishlistJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {

    public Wishlist toDomain(WishlistJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Wishlist domain = new Wishlist();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setUserId(entity.getUserId());
        domain.setServiceId(entity.getServiceId());
        return domain;
    }

    public WishlistJpaEntity toEntity(Wishlist domain) {
        if (domain == null) {
            return null;
        }
        WishlistJpaEntity entity = new WishlistJpaEntity();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setUserId(domain.getUserId());
        entity.setServiceId(domain.getServiceId());
        return entity;
    }

    public List<Wishlist> toDomainList(List<WishlistJpaEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).toList();
    }
}
