package com.danasea.backend.modules.service.infrastructure.mappers;

import com.danasea.backend.modules.service.domain.models.Wishlist;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.WishlistJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WishlistMapperTest {

    private WishlistMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WishlistMapper();
    }

    @Test
    void toDomain_mapsAllFieldsCorrectly() {
        WishlistJpaEntity entity = new WishlistJpaEntity();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setUserId(userId);
        entity.setServiceId(serviceId);

        Wishlist domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id, domain.getId());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
        assertEquals(userId, domain.getUserId());
        assertEquals(serviceId, domain.getServiceId());
    }

    @Test
    void toDomain_handlesNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    void toEntity_mapsAllFieldsCorrectly() {
        Wishlist domain = new Wishlist();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        domain.setId(id);
        domain.setUserId(userId);
        domain.setServiceId(serviceId);

        WishlistJpaEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals(userId, entity.getUserId());
        assertEquals(serviceId, entity.getServiceId());
    }

    @Test
    void toEntity_handlesNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toDomainList_mapsListProperly() {
        assertTrue(mapper.toDomainList(null).isEmpty());
        assertTrue(mapper.toDomainList(List.of()).isEmpty());

        WishlistJpaEntity entity = new WishlistJpaEntity();
        entity.setUserId(UUID.randomUUID());
        List<Wishlist> list = mapper.toDomainList(List.of(entity));
        assertEquals(1, list.size());
    }
}
