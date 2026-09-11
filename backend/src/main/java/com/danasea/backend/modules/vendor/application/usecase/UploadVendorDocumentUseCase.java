package com.danasea.backend.modules.vendor.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.danasea.backend.modules.vendor.application.port.DocumentStoragePort;
import com.danasea.backend.modules.vendor.domain.exception.InvalidDocTypeException;
import com.danasea.backend.modules.vendor.domain.exception.VendorNotFoundException;
import com.danasea.backend.modules.vendor.domain.models.DocStatus;
import com.danasea.backend.modules.vendor.domain.models.DocType;
import com.danasea.backend.modules.vendor.domain.models.VendorDocument;
import com.danasea.backend.modules.vendor.infrastructure.mapper.VendorDocumentMapper;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorDocumentJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorDocumentRepository;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UploadVendorDocumentUseCase {

    private final JpaVendorRepository jpaVendorRepository;
    private final JpaVendorDocumentRepository jpaVendorDocumentRepository;
    private final DocumentStoragePort documentStoragePort;
    private final VendorDocumentMapper vendorDocumentMapper;

    @Transactional
    public VendorDocument execute(UUID userId, MultipartFile file, String docTypeStr) {
        DocType docType = parseDocType(docTypeStr);
        return execute(userId, file, docType);
    }

    @Transactional
    public VendorDocument execute(UUID userId, MultipartFile file, DocType docType) {
        if (docType == null) {
            throw new InvalidDocTypeException("Document type cannot be null");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        VendorJpaEntity vendor = jpaVendorRepository.findByUserId(userId)
                .orElseThrow(VendorNotFoundException::new);

        String fileUrl = documentStoragePort.uploadDocument(file, "vendor_documents");

        VendorDocumentJpaEntity entity = new VendorDocumentJpaEntity();
        entity.setVendorId(vendor.getId());
        entity.setDocType(docType);
        entity.setFileUrl(fileUrl);
        entity.setStatus(DocStatus.PENDING);
        entity.setReviewedBy(null);
        entity.setReviewedAt(null);

        VendorDocumentJpaEntity saved = jpaVendorDocumentRepository.save(entity);
        return vendorDocumentMapper.toDomain(saved);
    }

    private DocType parseDocType(String docTypeStr) {
        if (docTypeStr == null || docTypeStr.isBlank()) {
            throw new InvalidDocTypeException("Document type is required");
        }
        try {
            return DocType.valueOf(docTypeStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidDocTypeException("Invalid document type: " + docTypeStr);
        }
    }
}
