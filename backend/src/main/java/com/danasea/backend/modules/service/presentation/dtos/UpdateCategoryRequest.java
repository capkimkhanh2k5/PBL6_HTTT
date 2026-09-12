package com.danasea.backend.modules.service.presentation.dtos;

import java.util.UUID;

public record UpdateCategoryRequest(
    String name,
    String nameEn,
    String slug,
    UUID parentId,
    String iconUrl
) {}
