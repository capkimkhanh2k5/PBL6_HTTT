package com.danasea.backend.shared.core.domain.models;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public abstract class BaseDomainModel {
    private UUID id;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
