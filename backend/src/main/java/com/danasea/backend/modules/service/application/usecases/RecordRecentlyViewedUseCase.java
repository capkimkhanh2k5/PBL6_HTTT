package com.danasea.backend.modules.service.application.usecases;

import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.RecentlyViewedRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecordRecentlyViewedUseCase {
    private final RecentlyViewedRepositoryPort recentlyViewedRepositoryPort;
    private final ServiceRepositoryPort serviceRepositoryPort;

    public void execute(UUID serviceId, UUID userId, String sessionId) {
        if (userId == null && (sessionId == null || sessionId.trim().isEmpty())) {
            return;
        }
        
        Optional<Service> serviceOpt = serviceRepositoryPort.findById(serviceId);
        if (serviceOpt.isEmpty()) return;

        Optional<RecentlyViewed> existing = Optional.empty();
        if (userId != null) {
            existing = recentlyViewedRepositoryPort.findFirstByUserIdAndServiceId(userId, serviceId);
        } else if (sessionId != null && !sessionId.trim().isEmpty()) {
            existing = recentlyViewedRepositoryPort.findFirstBySessionIdAndServiceId(sessionId, serviceId);
        }
        
        if (existing.isPresent()) {
            RecentlyViewed rv = existing.get();
            rv.setViewedAt(OffsetDateTime.now());
            recentlyViewedRepositoryPort.save(rv);
        } else {
            RecentlyViewed rv = new RecentlyViewed();
            rv.setId(UUID.randomUUID());
            rv.setServiceId(serviceId);
            rv.setUserId(userId);
            rv.setSessionId(sessionId);
            rv.setViewedAt(OffsetDateTime.now());
            recentlyViewedRepositoryPort.save(rv);
        }
    }
}
