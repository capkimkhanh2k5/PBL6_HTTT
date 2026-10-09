package com.danasea.backend.modules.service.presentation.controllers;

import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.application.usecases.CreateServiceOptionUseCase;
import com.danasea.backend.modules.service.application.usecases.GetVendorServiceOptionsUseCase;
import com.danasea.backend.modules.service.application.usecases.UpdateServiceOptionUseCase;
import com.danasea.backend.modules.service.presentation.dtos.CreateServiceOptionRequest;
import com.danasea.backend.modules.service.presentation.dtos.ServiceOptionResponse;
import com.danasea.backend.modules.service.presentation.dtos.UpdateServiceOptionRequest;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor/services/{id}/options")
@RequiredArgsConstructor
public class VendorServiceOptionController {

    private final GetVendorServiceOptionsUseCase getVendorServiceOptionsUseCase;
    private final CreateServiceOptionUseCase createServiceOptionUseCase;
    private final UpdateServiceOptionUseCase updateServiceOptionUseCase;

    @GetMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<List<ServiceOptionResponse>> getOptions(@PathVariable("id") UUID serviceId) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));
        List<ServiceOptionResponse> response = getVendorServiceOptionsUseCase.execute(userId, serviceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ServiceOptionResponse> createOption(
            @PathVariable("id") UUID serviceId,
            @Valid @RequestBody CreateServiceOptionRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));
        ServiceOptionResponse response = createServiceOptionUseCase.execute(userId, serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{optionId}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ServiceOptionResponse> updateOption(
            @PathVariable("id") UUID serviceId,
            @PathVariable("optionId") UUID optionId,
            @Valid @RequestBody UpdateServiceOptionRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));
        ServiceOptionResponse response = updateServiceOptionUseCase.execute(userId, serviceId, optionId, request);
        return ResponseEntity.ok(response);
    }
}
