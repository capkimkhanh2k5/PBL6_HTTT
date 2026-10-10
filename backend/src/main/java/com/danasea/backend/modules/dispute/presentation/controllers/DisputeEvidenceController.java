package com.danasea.backend.modules.dispute.presentation.controllers;

import com.danasea.backend.modules.dispute.application.usecases.UploadDisputeEvidenceUseCase;
import com.danasea.backend.modules.dispute.presentation.dtos.EvidenceUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/disputes/evidence")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class DisputeEvidenceController {

    private final UploadDisputeEvidenceUseCase uploadDisputeEvidenceUseCase;

    @PostMapping
    public ResponseEntity<EvidenceUploadResponse> uploadEvidence(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Received request to upload dispute evidence: filename={}",
                file != null ? file.getOriginalFilename() : "null");
        EvidenceUploadResponse response = uploadDisputeEvidenceUseCase.execute(file);
        return ResponseEntity.ok(response);
    }
}
