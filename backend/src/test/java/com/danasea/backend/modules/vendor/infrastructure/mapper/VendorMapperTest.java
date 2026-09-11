package com.danasea.backend.modules.vendor.infrastructure.mapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.vendor.domain.models.BadgeTier;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendorMapperTest {

    private VendorMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new VendorMapper();
    }

    @Test
    @DisplayName("toDomain with full entity")
    void toDomain_fullEntity() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        VendorJpaEntity entity = new VendorJpaEntity();
        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setUserId(userId);
        entity.setBusinessName("Sea Adventures Ltd");
        entity.setTaxCode("0123456789");
        entity.setAddress("123 Vo Nguyen Giap");
        entity.setBankAccountNumber("987654321");
        entity.setBankName("Vietcombank");
        entity.setBankAccountHolder("NGUYEN VAN A");
        entity.setVerificationStatus(VerificationStatus.APPROVED);
        entity.setRatingAvg(BigDecimal.valueOf(4.9));
        entity.setRatingCount(50);
        entity.setBadgeTier(BadgeTier.TOP_RATED);

        Vendor domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id, domain.getId());
        assertEquals(userId, domain.getUserId());
        assertEquals("Sea Adventures Ltd", domain.getBusinessName());
        assertEquals(VerificationStatus.APPROVED, domain.getVerificationStatus());
        assertEquals(BadgeTier.TOP_RATED, domain.getBadgeTier());
    }

    @Test
    @DisplayName("toDomain with null entity")
    void toDomain_null() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    @DisplayName("toEntity with full domain")
    void toEntity_fullDomain() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Vendor domain = new Vendor();
        domain.setId(id);
        domain.setUserId(userId);
        domain.setBusinessName("Blue Water Co");
        domain.setVerificationStatus(VerificationStatus.APPROVED);

        VendorJpaEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals(userId, entity.getUserId());
        assertEquals("Blue Water Co", entity.getBusinessName());
        assertEquals(VerificationStatus.APPROVED, entity.getVerificationStatus());
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

        VendorJpaEntity entity = new VendorJpaEntity();
        entity.setBusinessName("Test");
        List<Vendor> list = mapper.toDomainList(List.of(entity));
        assertEquals(1, list.size());
        assertEquals("Test", list.get(0).getBusinessName());
    }
}
