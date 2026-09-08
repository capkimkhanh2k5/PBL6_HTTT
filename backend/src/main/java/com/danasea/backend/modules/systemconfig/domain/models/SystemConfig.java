package com.danasea.backend.modules.systemconfig.domain.models;

import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemConfig extends BaseDomainModel {
    private String key;
    private String value;
    private String description;
    private UUID updatedBy;
}
