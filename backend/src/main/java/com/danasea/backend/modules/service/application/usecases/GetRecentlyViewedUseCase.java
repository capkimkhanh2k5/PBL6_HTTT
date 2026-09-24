package com.danasea.backend.modules.service.application.usecases;

import com.danasea.backend.modules.service.application.dtos.RecentlyViewedResult;
import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.RecentlyViewedRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetRecentlyViewedUseCase {
    private final RecentlyViewedRepositoryPort recentlyViewedRepositoryPort;
    private final ServiceRepositoryPort serviceRepositoryPort;
    private final ServiceImageRepositoryPort serviceImageRepositoryPort;

    public List<RecentlyViewedResult> execute(UUID userId, String sessionId) {
        List<RecentlyViewed> entities;
        if (userId != null) {
            entities = recentlyViewedRepositoryPort.findAllByUserId(userId);
        } else if (sessionId != null && !sessionId.trim().isEmpty()) {
            entities = recentlyViewedRepositoryPort.findAllBySessionId(sessionId);
        } else {
            return List.of();
        }

        return entities.stream().flatMap(rv ->
                serviceRepositoryPort.findPublishedById(rv.getServiceId()).stream().map(service -> {
            return RecentlyViewedResult.builder()
                .id(rv.getId())
                .serviceId(rv.getServiceId())
                .serviceName(service.getName())
                .primaryImageUrl(primaryImageUrl(service.getId()))
                .viewedAt(rv.getViewedAt() != null ? rv.getViewedAt().toLocalDateTime() : null)
                .build();
        })).collect(Collectors.toList());
    }

    private String primaryImageUrl(UUID serviceId) {
        return serviceImageRepositoryPort.findByServiceId(serviceId).stream()
                .findFirst()
                .map(image -> image.getUrl())
                .orElse(null);
    }
}
