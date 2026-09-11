package com.danasea.backend.modules.service.infrastructure.mapper;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceMapperTest {

    private ServiceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ServiceMapper();
    }

    @Test
    void toDomain_mapsAllFieldsCorrectly() {
        ServiceJpaEntity entity = new ServiceJpaEntity();
        UUID id = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setVendorId(vendorId);
        entity.setCategoryId(categoryId);
        entity.setName("Scuba Diving");
        entity.setNameEn("Scuba Diving EN");
        entity.setSlug("scuba-diving");
        entity.setDescription("Deep water diving experience");
        entity.setDescriptionEn("Deep water diving experience EN");
        entity.setPrice(BigDecimal.valueOf(120.50));
        entity.setDurationMinutes(120);
        entity.setCapacityPerSlot(10);
        entity.setLocationName("Son Tra Peninsula");
        entity.setAddress("Hoang Sa Road");
        entity.setLatitude(BigDecimal.valueOf(16.123));
        entity.setLongitude(BigDecimal.valueOf(108.456));
        entity.setStatus(ServiceStatus.PUBLISHED);
        entity.setWaiverContent("Standard waiver");
        entity.setWeatherSensitive(true);
        entity.setMinWindKmh(BigDecimal.valueOf(5));
        entity.setMaxWaveM(BigDecimal.valueOf(1.5));
        entity.setAvgRating(BigDecimal.valueOf(4.8));
        entity.setRatingCount(25);
        entity.setViewCount(100);

        Service domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id, domain.getId());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
        assertEquals(vendorId, domain.getVendorId());
        assertEquals(categoryId, domain.getCategoryId());
        assertEquals("Scuba Diving", domain.getName());
        assertEquals("Scuba Diving EN", domain.getNameEn());
        assertEquals("scuba-diving", domain.getSlug());
        assertEquals("Deep water diving experience", domain.getDescription());
        assertEquals("Deep water diving experience EN", domain.getDescriptionEn());
        assertEquals(BigDecimal.valueOf(120.50), domain.getPrice());
        assertEquals(120, domain.getDurationMinutes());
        assertEquals(10, domain.getCapacityPerSlot());
        assertEquals("Son Tra Peninsula", domain.getLocationName());
        assertEquals("Hoang Sa Road", domain.getAddress());
        assertEquals(BigDecimal.valueOf(16.123), domain.getLatitude());
        assertEquals(BigDecimal.valueOf(108.456), domain.getLongitude());
        assertEquals(ServiceStatus.PUBLISHED, domain.getStatus());
        assertEquals("Standard waiver", domain.getWaiverContent());
        assertTrue(domain.getWeatherSensitive());
        assertEquals(BigDecimal.valueOf(5), domain.getMinWindKmh());
        assertEquals(BigDecimal.valueOf(1.5), domain.getMaxWaveM());
        assertEquals(BigDecimal.valueOf(4.8), domain.getAvgRating());
        assertEquals(25, domain.getRatingCount());
        assertEquals(100, domain.getViewCount());
    }

    @Test
    void toDomain_handlesNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    void toEntity_mapsAllFieldsCorrectly() {
        Service domain = new Service();
        UUID id = UUID.randomUUID();
        domain.setId(id);
        domain.setName("Surfing");
        domain.setPrice(BigDecimal.valueOf(80));
        domain.setStatus(ServiceStatus.PUBLISHED);

        ServiceJpaEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("Surfing", entity.getName());
        assertEquals(BigDecimal.valueOf(80), entity.getPrice());
        assertEquals(ServiceStatus.PUBLISHED, entity.getStatus());
    }

    @Test
    void toEntity_handlesNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toDomainList_mapsListAndHandlesEmptyOrNull() {
        assertTrue(mapper.toDomainList(null).isEmpty());
        assertTrue(mapper.toDomainList(List.of()).isEmpty());

        ServiceJpaEntity entity = new ServiceJpaEntity();
        entity.setName("Test");
        List<Service> result = mapper.toDomainList(List.of(entity));
        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getName());
    }
}
