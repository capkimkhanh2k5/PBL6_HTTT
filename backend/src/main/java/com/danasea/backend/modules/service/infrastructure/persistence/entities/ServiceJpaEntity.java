package com.danasea.backend.modules.service.infrastructure.persistence.entities;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "services")
public class ServiceJpaEntity extends BaseJpaEntity {

    private UUID vendorId;

    private UUID categoryId;

    private String name;

    private String nameEn;

    private String slug;

    private String description;

    private String descriptionEn;

    private BigDecimal price;

    private Integer durationMinutes;

    private Integer capacityPerSlot;

    private String locationName;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    private ServiceStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    private String waiverContent;

    private Boolean weatherSensitive;

    private BigDecimal minWindKmh;

    private BigDecimal maxWaveM;

    private BigDecimal avgRating;

    private Integer ratingCount;

    private Integer viewCount;

}
