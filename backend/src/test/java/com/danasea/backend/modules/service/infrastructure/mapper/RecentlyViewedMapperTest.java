package com.danasea.backend.modules.service.infrastructure.mapper;

import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.RecentlyViewedJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecentlyViewedMapperTest {

    private RecentlyViewedMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RecentlyViewedMapper();
    }

    @Test
    void toDomain_mapsAllFieldsCorrectly() {
        RecentlyViewedJpaEntity entity = new RecentlyViewedJpaEntity();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String sessionId = "sess-12345";
        UUID serviceId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setServiceId(serviceId);
        entity.setViewedAt(now);

        RecentlyViewed domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id, domain.getId());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
        assertEquals(userId, domain.getUserId());
        assertEquals(sessionId, domain.getSessionId());
        assertEquals(serviceId, domain.getServiceId());
        assertEquals(now, domain.getViewedAt());
    }

    @Test
    void toDomain_handlesNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    void toEntity_mapsAllFieldsCorrectly() {
        RecentlyViewed domain = new RecentlyViewed();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String sessionId = "sess-abc";
        UUID serviceId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        domain.setId(id);
        domain.setUserId(userId);
        domain.setSessionId(sessionId);
        domain.setServiceId(serviceId);
        domain.setViewedAt(now);

        RecentlyViewedJpaEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals(userId, entity.getUserId());
        assertEquals(sessionId, entity.getSessionId());
        assertEquals(serviceId, entity.getServiceId());
        assertEquals(now, entity.getViewedAt());
    }

    @Test
    void toEntity_handlesNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toDomainList_mapsListProperly() {
        assertTrue(mapper.toDomainList(null).isEmpty());
        assertTrue(mapper.toDomainList(List.of()).isEmpty());

        RecentlyViewedJpaEntity entity = new RecentlyViewedJpaEntity();
        entity.setSessionId("sess-test");
        List<RecentlyViewed> list = mapper.toDomainList(List.of(entity));
        assertEquals(1, list.size());
        assertEquals("sess-test", list.get(0).getSessionId());
    }
}
