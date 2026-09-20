package com.danasea.backend.modules.weather.presentation.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.weather.application.dtos.CategorySafetyRuleResponse;
import com.danasea.backend.modules.weather.application.dtos.UpdateCategorySafetyRuleRequest;
import com.danasea.backend.modules.weather.domain.models.CategorySafetyRule;
import com.danasea.backend.modules.weather.domain.services.CategorySafetyRuleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/category-safety-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategorySafetyRuleController {

    private final CategorySafetyRuleService safetyRuleService;

    @GetMapping
    public ResponseEntity<List<CategorySafetyRuleResponse>> getAllCategorySafetyRules() {
        List<CategorySafetyRule> rules = safetyRuleService.getAllRules();
        List<CategorySafetyRuleResponse> response = rules.stream()
                .map(safetyRuleService::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategorySafetyRuleResponse> updateCategorySafetyRule(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategorySafetyRuleRequest request) {
        log.info("Admin request to update safety rule for categoryId: {}", categoryId);
        CategorySafetyRule updated = safetyRuleService.updateRule(categoryId, request);
        return ResponseEntity.ok(safetyRuleService.mapToResponse(updated));
    }
}
