package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.RecentlyViewedJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRecentlyViewedRepository extends JpaRepository<RecentlyViewedJpaEntity, UUID> {

    Optional<RecentlyViewedJpaEntity> findFirstByUserIdAndServiceId(UUID userId, UUID serviceId);

    Optional<RecentlyViewedJpaEntity> findFirstBySessionIdAndServiceId(String sessionId, UUID serviceId);

    List<RecentlyViewedJpaEntity> findAllByUserIdOrderByViewedAtDesc(UUID userId);

    List<RecentlyViewedJpaEntity> findAllBySessionIdOrderByViewedAtDesc(String sessionId);
}

