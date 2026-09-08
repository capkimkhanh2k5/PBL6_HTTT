package com.danasea.backend.modules.account.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "audit_logs")
public class AuditLogJpaEntity extends BaseJpaEntity {

    private UUID actorUserId;

    private String action;

    private String entityType;

    private UUID entityId;

    private String metadata;

}
