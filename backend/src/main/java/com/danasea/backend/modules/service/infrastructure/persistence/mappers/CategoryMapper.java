package com.danasea.backend.modules.service.infrastructure.persistence.mappers;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity;

@Component
public class CategoryMapper {

    public Category toDomain(CategoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Category domain = new Category();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setName(entity.getName());
        domain.setNameEn(entity.getNameEn());
        domain.setSlug(entity.getSlug());
        domain.setParentId(entity.getParentId());
        domain.setIconUrl(entity.getIconUrl());
        domain.setIsActive(entity.getIsActive());
        return domain;
    }

    public CategoryJpaEntity toEntity(Category domain) {
        if (domain == null) {
            return null;
        }
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setName(domain.getName());
        entity.setNameEn(domain.getNameEn());
        entity.setSlug(domain.getSlug());
        entity.setParentId(domain.getParentId());
        entity.setIconUrl(domain.getIconUrl());
        entity.setIsActive(domain.getIsActive() != null ? domain.getIsActive() : Boolean.TRUE);
        return entity;
    }

    public List<Category> toDomainList(List<CategoryJpaEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(this::toDomain)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<CategoryJpaEntity> toEntityList(List<Category> domains) {
        if (domains == null) {
            return Collections.emptyList();
        }
        return domains.stream()
                .map(this::toEntity)
                .filter(Objects::nonNull)
                .toList();
    }
}
