package com.danasea.backend.modules.service.presentation.dtos;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
    @NotBlank(message = "{validation.category.name.required}")
    String name,
    String nameEn,
    @NotBlank(message = "{validation.category.slug.required}")
    String slug,
    UUID parentId,
    String iconUrl
) {}
