package com.danasea.backend.modules.service.domain.models;

import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Category extends BaseDomainModel {
    private String name;
    private String nameEn;
    private String slug;
    private UUID parentId;
    private String iconUrl;
    private Boolean isActive;
    private Boolean requiresSafetyCert;
}
