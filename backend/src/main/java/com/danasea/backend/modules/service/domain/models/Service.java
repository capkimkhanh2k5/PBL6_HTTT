package com.danasea.backend.modules.service.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Service extends BaseDomainModel {
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
    private ServiceStatus status;
    private String waiverContent;
    private Boolean weatherSensitive;
    private BigDecimal minWindKmh;
    private BigDecimal maxWaveM;
    private BigDecimal avgRating;
    private Integer ratingCount;
    private Integer viewCount;
}
