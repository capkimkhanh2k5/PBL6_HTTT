package com.danasea.backend.modules.service.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.RecentlyViewed;

public interface RecentlyViewedRepositoryPort {

    Optional<RecentlyViewed> findFirstByUserIdAndServiceId(UUID userId, UUID serviceId);

    Optional<RecentlyViewed> findFirstBySessionIdAndServiceId(String sessionId, UUID serviceId);

    List<RecentlyViewed> findAllByUserId(UUID userId);

    List<RecentlyViewed> findAllBySessionId(String sessionId);

    RecentlyViewed save(RecentlyViewed recentlyViewed);

    Optional<RecentlyViewed> findById(UUID id);
}
