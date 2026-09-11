package com.danasea.backend.modules.service.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories", uniqueConstraints = {
    @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug")
})
public class CategoryJpaEntity extends BaseJpaEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String nameEn;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    private UUID parentId;

    @Column(columnDefinition = "TEXT")
    private String iconUrl;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(name = "requires_safety_cert", nullable = false)
    private Boolean requiresSafetyCert = false;

}
