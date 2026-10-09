package com.danasea.backend.modules.service.application.usecases;

import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.OptionStatus;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceAvailabilityPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceOptionRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.presentation.dtos.ServiceOptionResponse;
import com.danasea.backend.shared.i18n.LocalizedContentSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetPublicServiceDetailUseCase {
    private final ServiceRepositoryPort serviceRepositoryPort;
    private final RecordRecentlyViewedUseCase recordRecentlyViewedUseCase;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ServiceImageRepositoryPort serviceImageRepositoryPort;
    private final ServiceAvailabilityPort serviceAvailabilityPort;
    private final LocalizedContentSelector localizedContentSelector;
    private final ServiceOptionRepositoryPort serviceOptionRepositoryPort;

    public ServiceDetailResult execute(UUID id, UUID userId, String sessionId) {
        return load(id, userId, sessionId, true);
    }

    public ServiceDetailResult readOnlySnapshot(UUID id) {
        return load(id, null, null, false);
    }

    private ServiceDetailResult load(UUID id, UUID userId, String sessionId, boolean recordView) {
        Service service = serviceRepositoryPort.findPublishedById(id)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found or not published: " + id));

        if (recordView) {
            serviceRepositoryPort.incrementViewCount(id, ServiceStatus.PUBLISHED);
            recordRecentlyViewedUseCase.execute(id, userId, sessionId);
        }

        int viewCount = service.getViewCount() != null ? service.getViewCount() : 0;
        String categoryName = service.getCategoryId() == null ? null
                : categoryRepositoryPort.findById(service.getCategoryId())
                        .map(category -> category.getName())
                        .orElse(null);

        var options = serviceOptionRepositoryPort.findByServiceIdAndStatus(id, OptionStatus.ACTIVE).stream()
                .map(o -> ServiceOptionResponse.builder()
                        .id(o.getId())
                        .serviceId(o.getServiceId())
                        .name(o.getName())
                        .optionType(o.getOptionType())
                        .pricingUnit(o.getPricingUnit())
                        .price(o.getPrice())
                        .maxPaxPerPackage(o.getMaxPaxPerPackage())
                        .benefits(o.getBenefits())
                        .status(o.getStatus())
                        .createdAt(o.getCreatedAt())
                        .updatedAt(o.getUpdatedAt())
                        .build())
                .toList();

        return ServiceDetailResult.builder()
                .id(service.getId())
                .name(localize(service.getName(), service.getNameEn()))
                .description(localize(service.getDescription(), service.getDescriptionEn()))
                .price(service.getPrice())
                .address(service.getAddress())
                .latitude(service.getLatitude())
                .longitude(service.getLongitude())
                .averageRating(service.getAvgRating())
                .reviewCount(service.getRatingCount() != null ? service.getRatingCount() : 0)
                .viewCount(viewCount + (recordView ? 1 : 0))
                .categoryId(service.getCategoryId())
                .categoryName(categoryName)
                .imageUrls(serviceImageRepositoryPort.findByServiceId(id).stream()
                        .map(image -> image.getUrl())
                        .toList())
                .availableSlots(serviceAvailabilityPort.findAvailableSlots(id))
                .options(options)
                .build();
    }

    private String localize(String vietnamese, String english) {
        return localizedContentSelector == null ? vietnamese : localizedContentSelector.select(vietnamese, english);
    }
}
