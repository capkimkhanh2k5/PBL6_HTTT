package com.danasea.backend.modules.service.application.usecases;

import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetPublicServiceDetailUseCase {
    private final ServiceRepositoryPort serviceRepositoryPort;
    private final RecordRecentlyViewedUseCase recordRecentlyViewedUseCase;

    public ServiceDetailResult execute(UUID id, UUID userId, String sessionId) {
        Service service = serviceRepositoryPort.findPublishedById(id)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found or not published: " + id));

        serviceRepositoryPort.incrementViewCount(id, ServiceStatus.PUBLISHED);

        recordRecentlyViewedUseCase.execute(id, userId, sessionId);

        int viewCount = service.getViewCount() != null ? service.getViewCount() : 0;
        return ServiceDetailResult.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .address(service.getAddress())
                .latitude(service.getLatitude())
                .longitude(service.getLongitude())
                .averageRating(service.getAvgRating())
                .reviewCount(service.getRatingCount() != null ? service.getRatingCount() : 0)
                .viewCount(viewCount + 1)
                .categoryId(service.getCategoryId())
                .build();
    }
}
