package com.danasea.backend.modules.service.application.usecases;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.danasea.backend.modules.service.application.ports.FileStoragePort;
import com.danasea.backend.modules.service.domain.exceptions.InvalidFileTypeException;
import com.danasea.backend.modules.service.domain.exceptions.MaxImagesExceededException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceImageRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UploadServiceImageUseCase {

    private static final int MAX_IMAGES_PER_SERVICE = 10;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    private final JpaServiceRepository serviceRepository;
    private final JpaServiceImageRepository serviceImageRepository;
    private final FileStoragePort fileStoragePort;

    public ServiceImageJpaEntity execute(UUID serviceId, UUID vendorId, MultipartFile file) {
        // 1. Validate service exists
        var serviceEntity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + serviceId));

        // 2. Validate ownership (IDOR protection)
        if (!vendorId.equals(serviceEntity.getVendorId())) {
            throw new UnauthorizedServiceAccessException("Vendor does not own this service");
        }

        // 3. Validate file type
        if (file.isEmpty() || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidFileTypeException("Only image files are allowed (jpeg, png, webp, gif)");
        }

        // 4. Validate max images limit
        long currentCount = serviceImageRepository.countByServiceId(serviceId);
        if (currentCount >= MAX_IMAGES_PER_SERVICE) {
            throw new MaxImagesExceededException("Service cannot have more than " + MAX_IMAGES_PER_SERVICE + " images");
        }

        // 5. Upload to Cloudinary
        String folderPath = "services/" + serviceId + "/images";
        String uploadedUrl;
        try {
            uploadedUrl = fileStoragePort.uploadFile(file.getBytes(), file.getOriginalFilename(), folderPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }

        // 6. Compute next sort_order
        short nextSortOrder = serviceImageRepository.findMaxSortOrderByServiceId(serviceId)
                .map(max -> (short) (max + 1))
                .orElse((short) 1);

        // 7. Save entity
        var imageEntity = new ServiceImageJpaEntity();
        imageEntity.setServiceId(serviceId);
        imageEntity.setUrl(uploadedUrl);
        imageEntity.setSortOrder(nextSortOrder);

        return serviceImageRepository.save(imageEntity);
    }
}

