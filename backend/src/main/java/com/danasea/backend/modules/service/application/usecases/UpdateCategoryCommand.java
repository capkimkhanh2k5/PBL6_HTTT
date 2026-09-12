package com.danasea.backend.modules.service.application.usecases;

import java.util.UUID;

public record UpdateCategoryCommand(
    String name,
    String nameEn,
    String slug,
    UUID parentId,
    String iconUrl
) {}
