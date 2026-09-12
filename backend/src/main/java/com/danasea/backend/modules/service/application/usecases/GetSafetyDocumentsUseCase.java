package com.danasea.backend.modules.service.application.usecases;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSafetyDocumentJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSafetyDocumentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetSafetyDocumentsUseCase {

    private final JpaServiceRepository serviceRepository;
    private final JpaServiceSafetyDocumentRepository safetyDocumentRepository;

    public List<ServiceSafetyDocumentJpaEntity> execute(UUID serviceId) {
        // Validate service exists
        if (!serviceRepository.existsById(serviceId)) {
            throw new ServiceNotFoundException("Service not found: " + serviceId);
        }

        return safetyDocumentRepository.findByServiceId(serviceId);
    }
}
