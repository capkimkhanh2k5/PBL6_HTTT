package com.danasea.backend.modules.order.presentation.controllers;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.order.application.usecases.DiscountPreviewUseCase;
import com.danasea.backend.modules.order.presentation.dtos.DiscountPreviewRequest;
import com.danasea.backend.modules.order.presentation.dtos.DiscountPreviewResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutDiscountController {

    private final DiscountPreviewUseCase discountPreviewUseCase;

    public CheckoutDiscountController(DiscountPreviewUseCase discountPreviewUseCase) {
        this.discountPreviewUseCase = discountPreviewUseCase;
    }

    @PostMapping("/discount-preview")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<DiscountPreviewResponse> previewDiscount(@Valid @RequestBody DiscountPreviewRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        DiscountPreviewResponse response = discountPreviewUseCase.execute(userId, request);
        return ResponseEntity.ok(response);
    }
}
