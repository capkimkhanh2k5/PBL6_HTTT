package com.danasea.backend.modules.service.application.usecase;

import java.util.UUID;

public record UpdateCategoryCommand(
    String name,
    String nameEn,
    String slug,
    UUID parentId,
    String iconUrl
) {}
