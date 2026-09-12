package com.danasea.backend.modules.service.presentation.controllers;

import java.util.List;

import com.danasea.backend.modules.service.application.usecases.GetCategoryTreeUseCase;
import com.danasea.backend.modules.service.presentation.dtos.CategoryTreeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final GetCategoryTreeUseCase getCategoryTreeUseCase;

    @GetMapping
    public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
        return ResponseEntity.ok(getCategoryTreeUseCase.execute(false));
    }
}
