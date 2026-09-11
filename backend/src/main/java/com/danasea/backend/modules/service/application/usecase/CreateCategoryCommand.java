package com.danasea.backend.modules.service.application.usecase;

import java.util.UUID;

public record CreateCategoryCommand(
    UUID id,
    String name,
    String nameEn,
    String slug,
    UUID parentId,
    String iconUrl
) {
    public CreateCategoryCommand(String name, String nameEn, String slug, UUID parentId, String iconUrl) {
        this(null, name, nameEn, slug, parentId, iconUrl);
    }
}
