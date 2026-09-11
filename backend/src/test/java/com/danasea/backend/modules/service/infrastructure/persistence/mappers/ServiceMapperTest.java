package com.danasea.backend.modules.service.infrastructure.persistence.mappers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceMapperTest {

    private ServiceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ServiceMapper();
    }

    @Test
    @DisplayName("toDomain with full entity should map all fields correctly")
    void toDomain_fullEntity_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ServiceJpaEntity entity = new ServiceJpaEntity();
        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setVendorId(vendorId);
        entity.setCategoryId(categoryId);
        entity.setName("Scuba Diving Tour");
        entity.setNameEn("Scuba Diving Tour EN");
        entity.setSlug("scuba-diving-tour");
        entity.setDescription("Great experience");
        entity.setDescriptionEn("Great experience EN");
        entity.setPrice(BigDecimal.valueOf(1500000));
        entity.setDurationMinutes(120);
        entity.setCapacityPerSlot(10);
        entity.setLocationName("Son Tra Peninsula");
        entity.setAddress("Son Tra, Da Nang");
        entity.setLatitude(BigDecimal.valueOf(16.1));
        entity.setLongitude(BigDecimal.valueOf(108.2));
        entity.setStatus(ServiceStatus.PUBLISHED);
        entity.setRejectionReason(null);
        entity.setWaiverContent("Sign waiver");
        entity.setWeatherSensitive(true);
        entity.setMinWindKmh(BigDecimal.valueOf(15));
        entity.setMaxWaveM(BigDecimal.valueOf(2));
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
        assertEquals("Scuba Diving Tour", domain.getName());
        assertEquals("scuba-diving-tour", domain.getSlug());
        assertEquals(BigDecimal.valueOf(1500000), domain.getPrice());
        assertEquals(ServiceStatus.PUBLISHED, domain.getStatus());
        assertTrue(domain.getWeatherSensitive());
        assertEquals(100, domain.getViewCount());
    }

    @Test
    @DisplayName("toDomain with null entity should return null")
    void toDomain_null_returnsNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    @DisplayName("toEntity with full domain should map all fields correctly")
    void toEntity_fullDomain_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Service domain = Service.builder()
                .id(id)
                .createdAt(now)
                .updatedAt(now)
                .vendorId(vendorId)
                .categoryId(categoryId)
                .name("Snorkeling Trip")
                .nameEn("Snorkeling Trip EN")
                .slug("snorkeling-trip")
                .description("Fun in the sun")
                .descriptionEn("Fun in the sun EN")
                .price(BigDecimal.valueOf(500000))
                .durationMinutes(60)
                .capacityPerSlot(5)
                .locationName("Cu Lao Cham")
                .address("Tan Hiep")
                .latitude(BigDecimal.valueOf(15.9))
                .longitude(BigDecimal.valueOf(108.5))
                .status(ServiceStatus.REJECTED)
                .rejectionReason("Unclear itinerary")
                .waiverContent("Waiver form")
                .weatherSensitive(true)
                .minWindKmh(BigDecimal.valueOf(20))
                .maxWaveM(BigDecimal.valueOf(3))
                .avgRating(BigDecimal.valueOf(4.2))
                .ratingCount(10)
                .viewCount(50)
                .build();

        ServiceJpaEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals(vendorId, entity.getVendorId());
        assertEquals(categoryId, entity.getCategoryId());
        assertEquals("Snorkeling Trip", entity.getName());
        assertEquals(ServiceStatus.REJECTED, entity.getStatus());
        assertEquals("Unclear itinerary", entity.getRejectionReason());
        assertTrue(entity.getWeatherSensitive());
    }

    @Test
    @DisplayName("toEntity should apply safe defaults when fields are null")
    void toEntity_nullFields_appliesDefaults() {
        Service minimal = Service.builder().build();

        ServiceJpaEntity entity = mapper.toEntity(minimal);

        assertNotNull(entity);
        assertEquals(ServiceStatus.DRAFT, entity.getStatus());
        assertFalse(entity.getWeatherSensitive());
        assertEquals(BigDecimal.ZERO, entity.getAvgRating());
        assertEquals(0, entity.getRatingCount());
        assertEquals(0, entity.getViewCount());
    }

    @Test
    @DisplayName("toEntity with null domain should return null")
    void toEntity_null_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    @DisplayName("toDomainList and toEntityList handling")
    void listMapping() {
        assertTrue(mapper.toDomainList(null).isEmpty());
        assertTrue(mapper.toDomainList(Collections.emptyList()).isEmpty());
        assertTrue(mapper.toEntityList(null).isEmpty());
        assertTrue(mapper.toEntityList(Collections.emptyList()).isEmpty());

        ServiceJpaEntity entity = new ServiceJpaEntity();
        entity.setName("Test Service");
        List<Service> domains = mapper.toDomainList(List.of(entity));
        assertEquals(1, domains.size());
        assertEquals("Test Service", domains.get(0).getName());

        Service domain = Service.builder().name("Test Domain").build();
        List<ServiceJpaEntity> entities = mapper.toEntityList(List.of(domain));
        assertEquals(1, entities.size());
        assertEquals("Test Domain", entities.get(0).getName());
    }
}
