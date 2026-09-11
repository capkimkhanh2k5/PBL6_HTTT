package com.danasea.backend.modules.service.infrastructure.persistence.mappers;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceImageMapperTest {

    private ServiceImageMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ServiceImageMapper();
    }

    @Test
    @DisplayName("toDomain with full entity")
    void toDomain_fullEntity() {
        UUID id = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        ServiceImageJpaEntity entity = new ServiceImageJpaEntity();
        entity.setId(id);
        entity.setServiceId(serviceId);
        entity.setUrl("https://image.png");
        entity.setSortOrder((short) 1);

        ServiceImage domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id, domain.getId());
        assertEquals(serviceId, domain.getServiceId());
        assertEquals("https://image.png", domain.getUrl());
        assertEquals((short) 1, domain.getSortOrder());
    }

    @Test
    @DisplayName("toDomain with null entity")
    void toDomain_null() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    @DisplayName("toEntity with full domain and defaults")
    void toEntity_fullDomain() {
        UUID serviceId = UUID.randomUUID();
        ServiceImage domain = ServiceImage.builder()
                .serviceId(serviceId)
                .url("https://photo.jpg")
                .build();

        ServiceImageJpaEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(serviceId, entity.getServiceId());
        assertEquals("https://photo.jpg", entity.getUrl());
        assertEquals((short) 0, entity.getSortOrder()); // default 0
    }

    @Test
    @DisplayName("toEntity with null domain")
    void toEntity_null() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    @DisplayName("List mapping")
    void listMapping() {
        assertTrue(mapper.toDomainList(null).isEmpty());
        assertTrue(mapper.toDomainList(Collections.emptyList()).isEmpty());
        assertTrue(mapper.toEntityList(null).isEmpty());
        assertTrue(mapper.toEntityList(Collections.emptyList()).isEmpty());

        ServiceImageJpaEntity entity = new ServiceImageJpaEntity();
        entity.setUrl("https://img.png");
        List<ServiceImage> list = mapper.toDomainList(List.of(entity));
        assertEquals(1, list.size());
        assertEquals("https://img.png", list.get(0).getUrl());
    }
}
