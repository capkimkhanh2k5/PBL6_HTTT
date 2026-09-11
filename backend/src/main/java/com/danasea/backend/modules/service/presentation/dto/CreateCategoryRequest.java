package com.danasea.backend.modules.service.presentation.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
    @NotBlank(message = "Category name must not be blank")
    String name,
    String nameEn,
    @NotBlank(message = "Category slug must not be blank")
    String slug,
    UUID parentId,
    String iconUrl
) {}
