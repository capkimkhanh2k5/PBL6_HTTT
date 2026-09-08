package com.danasea.backend.modules.systemconfig.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "system_configs")
public class SystemConfigJpaEntity extends BaseJpaEntity {

    private String key;

    private String value;

    private String description;

    private UUID updatedBy;

}
