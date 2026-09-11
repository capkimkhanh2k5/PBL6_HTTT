package com.danasea.backend.modules.service.presentation.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CategoryTreeResponse(
    UUID id,
    String name,
    String nameEn,
    String slug,
    UUID parentId,
    String iconUrl,
    Boolean isActive,
    List<CategoryTreeResponse> children
) {
    public CategoryTreeResponse {
        if (children == null) {
            children = new ArrayList<>();
        }
    }
}
