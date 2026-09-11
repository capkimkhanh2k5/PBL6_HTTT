package com.danasea.backend.modules.service.application.usecase;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.service.domain.exceptions.SafetyDocumentRequiredException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.DocStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSafetyDocumentJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaCategoryRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSafetyDocumentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApproveSafetyDocumentUseCase {

    private final JpaServiceSafetyDocumentRepository safetyDocumentRepository;
    private final JpaServiceRepository serviceRepository;
    private final JpaCategoryRepository categoryRepository;

    public ServiceSafetyDocumentJpaEntity approve(UUID documentId, UUID adminId) {
        var document = safetyDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Safety document not found: " + documentId));

        document.setStatus(DocStatus.APPROVED);
        document.setReviewedBy(adminId);
        document.setReviewedAt(OffsetDateTime.now());
        document.setRejectionReason(null);

        return safetyDocumentRepository.save(document);
    }

    public ServiceSafetyDocumentJpaEntity reject(UUID documentId, UUID adminId, String rejectionReason) {
        var document = safetyDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Safety document not found: " + documentId));

        document.setStatus(DocStatus.REJECTED);
        document.setReviewedBy(adminId);
        document.setReviewedAt(OffsetDateTime.now());
        document.setRejectionReason(rejectionReason);

        return safetyDocumentRepository.save(document);
    }

    public boolean canPublish(UUID serviceId) {
        // 1. Load service
        var service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + serviceId));

        // 2. Load category
        var category = categoryRepository.findById(service.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + service.getCategoryId()));

        // 3. Check if safety cert is required by category flag or weather sensitivity
        boolean requiresCert = Boolean.TRUE.equals(service.getWeatherSensitive())
                || Boolean.TRUE.equals(category.getRequiresSafetyCert());

        if (!requiresCert) {
            // Normal service — no safety doc needed
            return true;
        }

        // 4. Check that at least one approved safety document exists
        boolean hasApprovedDoc = safetyDocumentRepository
                .existsByServiceIdAndStatus(serviceId, DocStatus.APPROVED);

        if (!hasApprovedDoc) {
            throw new SafetyDocumentRequiredException(
                    "High-risk services require an approved safety document before publishing.");
        }

        return true;
    }
}

