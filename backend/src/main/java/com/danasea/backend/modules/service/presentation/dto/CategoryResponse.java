package com.danasea.backend.modules.service.presentation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String name,
    String nameEn,
    String slug,
    UUID parentId,
    String iconUrl,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
