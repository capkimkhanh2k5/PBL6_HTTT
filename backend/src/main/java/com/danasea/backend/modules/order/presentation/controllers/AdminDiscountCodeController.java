package com.danasea.backend.modules.order.presentation.controllers;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.order.application.usecases.AdminDiscountCodeUseCase;
import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.presentation.dtos.CreateDiscountCodeRequest;
import com.danasea.backend.modules.order.presentation.dtos.DiscountCodeResponse;
import com.danasea.backend.modules.order.presentation.dtos.UpdateDiscountCodeRequest;

@RestController
@RequestMapping("/api/admin/discount-codes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDiscountCodeController {

    private final AdminDiscountCodeUseCase adminDiscountCodeUseCase;

    public AdminDiscountCodeController(AdminDiscountCodeUseCase adminDiscountCodeUseCase) {
        this.adminDiscountCodeUseCase = adminDiscountCodeUseCase;
    }

    @GetMapping
    public ResponseEntity<Page<DiscountCodeResponse>> getDiscountCodes(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) DiscountScope scope,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<DiscountCodeResponse> page = adminDiscountCodeUseCase.getDiscountCodes(code, scope, vendorId, isActive, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscountCodeResponse> getDiscountCodeById(@PathVariable UUID id) {
        DiscountCodeResponse response = adminDiscountCodeUseCase.getDiscountCodeById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<DiscountCodeResponse> createDiscountCode(@Valid @RequestBody CreateDiscountCodeRequest request) {
        DiscountCodeResponse response = adminDiscountCodeUseCase.createDiscountCode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DiscountCodeResponse> updateDiscountCode(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDiscountCodeRequest request
    ) {
        DiscountCodeResponse response = adminDiscountCodeUseCase.updateDiscountCode(id, request);
        return ResponseEntity.ok(response);
    }
}
