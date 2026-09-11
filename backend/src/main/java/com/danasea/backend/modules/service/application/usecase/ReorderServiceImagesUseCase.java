package com.danasea.backend.modules.service.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.service.domain.exceptions.ImageNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceImageRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReorderServiceImagesUseCase {

    private final JpaServiceRepository serviceRepository;
    private final JpaServiceImageRepository serviceImageRepository;

    public List<ServiceImageJpaEntity> execute(UUID serviceId, UUID vendorId, List<UUID> imageIds) {
        // 1. Validate service exists
        var serviceEntity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + serviceId));

        // 2. Validate ownership
        if (!vendorId.equals(serviceEntity.getVendorId())) {
            throw new UnauthorizedServiceAccessException("Vendor does not own this service");
        }

        // 3. Load all images belonging to this service
        List<ServiceImageJpaEntity> existingImages =
                serviceImageRepository.findByServiceIdOrderBySortOrderAsc(serviceId);

        // 4. Build lookup map (id -> entity) — only for images of this service (IDOR protection)
        Map<UUID, ServiceImageJpaEntity> imageMap = existingImages.stream()
                .collect(Collectors.toMap(ServiceImageJpaEntity::getId, Function.identity()));

        // 5. Validate all provided IDs belong to this service
        for (UUID imageId : imageIds) {
            if (!imageMap.containsKey(imageId)) {
                throw new ImageNotFoundException("Image " + imageId + " not found in service " + serviceId);
            }
        }

        // 5b. Validate count matches (no missing or extra IDs)
        if (imageIds.size() != existingImages.size()) {
            throw new IllegalArgumentException(
                    "imageIds count (" + imageIds.size() + ") does not match service image count (" + existingImages.size() + ")");
        }

        // 5c. Validate no duplicates
        long distinctCount = imageIds.stream().distinct().count();
        if (distinctCount != imageIds.size()) {
            throw new IllegalArgumentException("imageIds contains duplicate entries");
        }

        // 6. Reassign sort_order based on given order
        List<ServiceImageJpaEntity> toUpdate = new ArrayList<>();
        for (int i = 0; i < imageIds.size(); i++) {
            ServiceImageJpaEntity image = imageMap.get(imageIds.get(i));
            image.setSortOrder((short) (i + 1));
            toUpdate.add(image);
        }

        return serviceImageRepository.saveAll(toUpdate);
    }
}

