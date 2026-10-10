package com.danasea.backend.modules.dispute.application.usecases;

import com.danasea.backend.modules.dispute.presentation.dtos.EvidenceUploadResponse;
import com.danasea.backend.modules.service.application.ports.FileStoragePort;
import com.danasea.backend.modules.service.application.usecases.helpers.FileSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadDisputeEvidenceUseCase {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final String STORAGE_FOLDER = "disputes/evidence";

    private final FileStoragePort fileStoragePort;

    public EvidenceUploadResponse execute(MultipartFile file) {
        log.info("Received dispute evidence upload request: filename={}, size={}",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0);

        byte[] validatedBytes = FileSignatureValidator.readAndValidate(
                file,
                ALLOWED_CONTENT_TYPES,
                MAX_FILE_SIZE
        );

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "evidence";
        }

        String fileUrl = fileStoragePort.uploadFile(validatedBytes, originalFilename, STORAGE_FOLDER);
        log.info("Dispute evidence successfully stored at URL: {}", fileUrl);

        return new EvidenceUploadResponse(fileUrl);
    }
}
