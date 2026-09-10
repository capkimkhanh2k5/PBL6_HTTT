package com.danasea.backend.modules.audit.infrastructure.mapper;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.audit.domain.models.AuditLog;
import com.danasea.backend.modules.audit.infrastructure.persistence.entities.AuditLogJpaEntity;

@Component
public class AuditLogMapper {

    public AuditLog toDomain(AuditLogJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        AuditLog domain = new AuditLog();
        domain.setId(entity.getId());
        domain.setActorUserId(entity.getActorUserId());
        domain.setAction(entity.getAction());
        domain.setEntityType(entity.getEntityType());
        domain.setEntityId(entity.getEntityId());
        domain.setMetadata(entity.getMetadata());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public AuditLogJpaEntity toEntity(AuditLog domain) {
        if (domain == null) {
            return null;
        }
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setId(domain.getId());
        entity.setActorUserId(domain.getActorUserId());
        entity.setAction(domain.getAction());
        entity.setEntityType(domain.getEntityType());
        entity.setEntityId(domain.getEntityId());
        entity.setMetadata(domain.getMetadata());
        return entity;
    }
}
