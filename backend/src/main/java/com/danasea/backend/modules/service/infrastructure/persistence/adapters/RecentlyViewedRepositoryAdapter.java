package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.domain.ports.RecentlyViewedRepositoryPort;
import com.danasea.backend.modules.service.infrastructure.mapper.RecentlyViewedMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.RecentlyViewedJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaRecentlyViewedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RecentlyViewedRepositoryAdapter implements RecentlyViewedRepositoryPort {

    private final JpaRecentlyViewedRepository jpaRecentlyViewedRepository;
    private final RecentlyViewedMapper recentlyViewedMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<RecentlyViewed> findFirstByUserIdAndServiceId(UUID userId, UUID serviceId) {
        return jpaRecentlyViewedRepository.findFirstByUserIdAndServiceId(userId, serviceId)
                .map(recentlyViewedMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecentlyViewed> findFirstBySessionIdAndServiceId(String sessionId, UUID serviceId) {
        return jpaRecentlyViewedRepository.findFirstBySessionIdAndServiceId(sessionId, serviceId)
                .map(recentlyViewedMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentlyViewed> findAllByUserId(UUID userId) {
        return jpaRecentlyViewedRepository.findAllByUserIdOrderByViewedAtDesc(userId)
                .stream()
                .map(recentlyViewedMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentlyViewed> findAllBySessionId(String sessionId) {
        return jpaRecentlyViewedRepository.findAllBySessionIdOrderByViewedAtDesc(sessionId)
                .stream()
                .map(recentlyViewedMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public RecentlyViewed save(RecentlyViewed recentlyViewed) {
        RecentlyViewedJpaEntity entity = recentlyViewedMapper.toEntity(recentlyViewed);
        RecentlyViewedJpaEntity saved = jpaRecentlyViewedRepository.save(entity);
        return recentlyViewedMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecentlyViewed> findById(UUID id) {
        return jpaRecentlyViewedRepository.findById(id)
                .map(recentlyViewedMapper::toDomain);
    }
}
