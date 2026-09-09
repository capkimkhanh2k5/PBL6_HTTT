package com.danasea.backend.shared.core.domain.models;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class BaseDomainModel {
    private UUID id;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
