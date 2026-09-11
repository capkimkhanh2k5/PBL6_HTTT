package com.danasea.backend.modules.service.domain.models;

import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Category extends BaseDomainModel {
    private String name;
    private String nameEn;
    private String slug;
    private UUID parentId;
    private String iconUrl;
    private Boolean isActive;

    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }
}
