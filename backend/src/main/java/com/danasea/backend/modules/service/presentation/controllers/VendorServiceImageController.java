package com.danasea.backend.modules.service.presentation.controllers;

import com.danasea.backend.security.infrastructure.SecurityUtils;
import com.danasea.backend.modules.service.application.usecase.DeleteServiceImageUseCase;
import com.danasea.backend.modules.service.application.usecase.ReorderServiceImagesUseCase;
import com.danasea.backend.modules.service.application.usecase.UploadServiceImageUseCase;
import com.danasea.backend.modules.service.presentation.dto.ReorderImagesRequest;
import com.danasea.backend.modules.service.presentation.dto.ServiceImageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/services/{serviceId}/images")
@RequiredArgsConstructor
public class VendorServiceImageController {

    private final UploadServiceImageUseCase uploadServiceImageUseCase;
    private final DeleteServiceImageUseCase deleteServiceImageUseCase;
    private final ReorderServiceImagesUseCase reorderServiceImagesUseCase;

    /**
     * POST /api/vendor/services/{serviceId}/images
     * Upload ảnh cho service — multipart/form-data
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ServiceImageResponse> uploadImage(
            @PathVariable UUID serviceId,
            @RequestParam("file") MultipartFile file,
            Principal principal) {

        UUID vendorId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User ID not found"));
        var saved = uploadServiceImageUseCase.execute(serviceId, vendorId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ServiceImageResponse.from(saved));
    }

    /**
     * DELETE /api/vendor/services/{serviceId}/images/{imageId}
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID serviceId,
            @PathVariable UUID imageId,
            Principal principal) {

        UUID vendorId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User ID not found"));
        deleteServiceImageUseCase.execute(serviceId, imageId, vendorId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/vendor/services/{serviceId}/images/reorder
     * Đổi sort_order hàng loạt — body: { "imageIds": ["uuid1", "uuid2", ...] }
     */
    @PatchMapping("/reorder")
    public ResponseEntity<List<ServiceImageResponse>> reorderImages(
            @PathVariable UUID serviceId,
            @Valid @RequestBody ReorderImagesRequest request,
            Principal principal) {

        UUID vendorId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User ID not found"));
        var reordered = reorderServiceImagesUseCase.execute(serviceId, vendorId, request.imageIds());
        return ResponseEntity.ok(reordered.stream().map(ServiceImageResponse::from).toList());
    }
}
