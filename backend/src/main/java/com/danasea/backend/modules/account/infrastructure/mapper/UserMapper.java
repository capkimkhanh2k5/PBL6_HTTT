package com.danasea.backend.modules.account.infrastructure.mapper;

import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) return null;
        User domain = new User();
        domain.setId(entity.getId());
        domain.setEmail(entity.getEmail());
        domain.setPhone(entity.getPhone());
        domain.setPasswordHash(entity.getPasswordHash());
        domain.setFullName(entity.getFullName());
        domain.setRole(entity.getRole());
        domain.setAvatarUrl(entity.getAvatarUrl());
        domain.setIsEmailVerified(entity.getIsEmailVerified());
        domain.setIsLocked(entity.getIsLocked());
        domain.setLocale(entity.getLocale());
        return domain;
    }

    public UserJpaEntity toEntity(User domain) {
        if (domain == null) return null;
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setEmail(domain.getEmail());
        entity.setPhone(domain.getPhone());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setFullName(domain.getFullName());
        entity.setRole(domain.getRole());
        entity.setAvatarUrl(domain.getAvatarUrl());
        entity.setIsEmailVerified(domain.getIsEmailVerified());
        entity.setIsLocked(domain.getIsLocked());
        entity.setLocale(domain.getLocale());
        return entity;
    }
}
