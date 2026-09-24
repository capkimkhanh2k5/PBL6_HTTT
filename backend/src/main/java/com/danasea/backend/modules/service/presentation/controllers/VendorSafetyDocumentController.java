package com.danasea.backend.modules.service.presentation.controllers;

import com.danasea.backend.security.infrastructure.SecurityUtils;
import com.danasea.backend.modules.service.application.usecases.UploadSafetyDocumentUseCase;
import com.danasea.backend.modules.service.presentation.dtos.SafetyDocumentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import com.danasea.backend.modules.service.domain.ports.VendorPort;

import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/services/{serviceId}/safety-documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorSafetyDocumentController {

    private final UploadSafetyDocumentUseCase uploadSafetyDocumentUseCase;
    private final VendorPort vendorPort;

    /**
     * POST /api/vendor/services/{serviceId}/safety-documents
     * Upload chứng chỉ an toàn (PDF hoặc ảnh)
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<SafetyDocumentResponse> uploadSafetyDocument(
            @PathVariable UUID serviceId,
            @RequestParam("file") MultipartFile file) {

        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User ID not found"));
        UUID vendorId = vendorPort.findByUserId(userId)
                .map(vendor -> vendor.getId())
                .orElseThrow(() -> new AccessDeniedException("Vendor profile not found"));
        var saved = uploadSafetyDocumentUseCase.execute(serviceId, vendorId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(SafetyDocumentResponse.from(saved));
    }
}
