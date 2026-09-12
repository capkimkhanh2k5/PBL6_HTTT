package com.danasea.backend.modules.service.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.service.application.ports.FileStoragePort;
import com.danasea.backend.modules.service.domain.exceptions.ImageNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceImageRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteServiceImageUseCase {

    private final JpaServiceRepository serviceRepository;
    private final JpaServiceImageRepository serviceImageRepository;
    private final FileStoragePort fileStoragePort;

    public void execute(UUID serviceId, UUID imageId, UUID vendorId) {
        // 1. Validate service exists
        var serviceEntity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + serviceId));

        // 2. Validate ownership (IDOR protection)
        if (!vendorId.equals(serviceEntity.getVendorId())) {
            throw new UnauthorizedServiceAccessException("Vendor does not own this service");
        }

        // 3. Find image — must belong to this service (IDOR protection)
        var imageEntity = serviceImageRepository.findByIdAndServiceId(imageId, serviceId)
                .orElseThrow(() -> new ImageNotFoundException(
                        "Image " + imageId + " not found in service " + serviceId));

        // 4. Delete from Cloudinary
        fileStoragePort.deleteFile(imageEntity.getUrl());

        // 5. Delete from DB
        serviceImageRepository.deleteByIdAndServiceId(imageId, serviceId);
    }
}
