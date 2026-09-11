package com.danasea.backend.modules.service.presentation.controller;

import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.application.usecase.CreateCategoryUseCase;
import com.danasea.backend.modules.service.application.usecase.DeactivateCategoryUseCase;
import com.danasea.backend.modules.service.application.usecase.GetCategoryTreeUseCase;
import com.danasea.backend.modules.service.application.usecase.UpdateCategoryUseCase;
import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.infrastructure.mapper.CategoryMapper;
import com.danasea.backend.modules.service.presentation.dto.CategoryResponse;
import com.danasea.backend.modules.service.presentation.dto.CategoryTreeResponse;
import com.danasea.backend.modules.service.presentation.dto.CreateCategoryRequest;
import com.danasea.backend.modules.service.presentation.dto.UpdateCategoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final GetCategoryTreeUseCase getCategoryTreeUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeactivateCategoryUseCase deactivateCategoryUseCase;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public ResponseEntity<List<CategoryTreeResponse>> getAdminCategoryTree() {
        return ResponseEntity.ok(getCategoryTreeUseCase.execute(true));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        Category category = createCategoryUseCase.execute(
                request.name(),
                request.nameEn(),
                request.slug(),
                request.parentId(),
                request.iconUrl()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryMapper.toResponse(category));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @RequestBody UpdateCategoryRequest request) {
        Category category = updateCategoryUseCase.execute(
                id,
                request.name(),
                request.nameEn(),
                request.slug(),
                request.parentId(),
                request.iconUrl()
        );
        return ResponseEntity.ok(categoryMapper.toResponse(category));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CategoryResponse> deactivateCategory(@PathVariable UUID id) {
        Category category = deactivateCategoryUseCase.execute(id);
        return ResponseEntity.ok(categoryMapper.toResponse(category));
    }
}
