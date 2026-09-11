package com.danasea.backend.modules.service.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "categorys")
public class CategoryJpaEntity extends BaseJpaEntity {

    private String name;

    private String nameEn;

    private String slug;

    private UUID parentId;

    private String iconUrl;

    private Boolean isActive;

    @Column(name = "requires_safety_cert", nullable = false)
    private Boolean requiresSafetyCert = false;

}
