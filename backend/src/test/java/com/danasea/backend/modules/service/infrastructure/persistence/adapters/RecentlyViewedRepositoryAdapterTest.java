package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.infrastructure.mappers.RecentlyViewedMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.RecentlyViewedJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaRecentlyViewedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecentlyViewedRepositoryAdapterTest {

    private JpaRecentlyViewedRepository jpaRecentlyViewedRepository;
    private RecentlyViewedMapper recentlyViewedMapper;
    private RecentlyViewedRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaRecentlyViewedRepository = mock(JpaRecentlyViewedRepository.class);
        recentlyViewedMapper = mock(RecentlyViewedMapper.class);
        adapter = new RecentlyViewedRepositoryAdapter(jpaRecentlyViewedRepository, recentlyViewedMapper);
    }

    @Test
    void findFirstByUserIdAndServiceId_delegates() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        RecentlyViewedJpaEntity entity = new RecentlyViewedJpaEntity();
        RecentlyViewed domain = new RecentlyViewed();

        when(jpaRecentlyViewedRepository.findFirstByUserIdAndServiceId(userId, serviceId)).thenReturn(Optional.of(entity));
        when(recentlyViewedMapper.toDomain(entity)).thenReturn(domain);

        Optional<RecentlyViewed> result = adapter.findFirstByUserIdAndServiceId(userId, serviceId);

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
    }

    @Test
    void findFirstBySessionIdAndServiceId_delegates() {
        String sessionId = "sess-123";
        UUID serviceId = UUID.randomUUID();
        RecentlyViewedJpaEntity entity = new RecentlyViewedJpaEntity();
        RecentlyViewed domain = new RecentlyViewed();

        when(jpaRecentlyViewedRepository.findFirstBySessionIdAndServiceId(sessionId, serviceId)).thenReturn(Optional.of(entity));
        when(recentlyViewedMapper.toDomain(entity)).thenReturn(domain);

        Optional<RecentlyViewed> result = adapter.findFirstBySessionIdAndServiceId(sessionId, serviceId);

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
    }

    @Test
    void findAllByUserId_returnsMappedList() {
        UUID userId = UUID.randomUUID();
        RecentlyViewedJpaEntity entity = new RecentlyViewedJpaEntity();
        RecentlyViewed domain = new RecentlyViewed();

        when(jpaRecentlyViewedRepository.findAllByUserIdOrderByViewedAtDesc(userId)).thenReturn(List.of(entity));
        when(recentlyViewedMapper.toDomain(entity)).thenReturn(domain);

        List<RecentlyViewed> result = adapter.findAllByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(domain, result.get(0));
    }

    @Test
    void findAllBySessionId_returnsMappedList() {
        String sessionId = "sess-1";
        RecentlyViewedJpaEntity entity = new RecentlyViewedJpaEntity();
        RecentlyViewed domain = new RecentlyViewed();

        when(jpaRecentlyViewedRepository.findAllBySessionIdOrderByViewedAtDesc(sessionId)).thenReturn(List.of(entity));
        when(recentlyViewedMapper.toDomain(entity)).thenReturn(domain);

        List<RecentlyViewed> result = adapter.findAllBySessionId(sessionId);

        assertEquals(1, result.size());
        assertEquals(domain, result.get(0));
    }

    @Test
    void save_mapsAndSaves() {
        RecentlyViewed domain = new RecentlyViewed();
        RecentlyViewedJpaEntity entity = new RecentlyViewedJpaEntity();

        when(recentlyViewedMapper.toEntity(domain)).thenReturn(entity);
        when(jpaRecentlyViewedRepository.save(entity)).thenReturn(entity);
        when(recentlyViewedMapper.toDomain(entity)).thenReturn(domain);

        RecentlyViewed saved = adapter.save(domain);

        assertNotNull(saved);
        verify(jpaRecentlyViewedRepository).save(entity);
    }

    @Test
    void findById_returnsMappedDomain() {
        UUID id = UUID.randomUUID();
        RecentlyViewedJpaEntity entity = new RecentlyViewedJpaEntity();
        RecentlyViewed domain = new RecentlyViewed();

        when(jpaRecentlyViewedRepository.findById(id)).thenReturn(Optional.of(entity));
        when(recentlyViewedMapper.toDomain(entity)).thenReturn(domain);

        Optional<RecentlyViewed> result = adapter.findById(id);

        assertTrue(result.isPresent());
    }
}
