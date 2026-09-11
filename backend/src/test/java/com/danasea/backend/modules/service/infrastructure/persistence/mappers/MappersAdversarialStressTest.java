package com.danasea.backend.modules.service.infrastructure.persistence.mappers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.vendor.application.service.VendorInternalService;
import com.danasea.backend.modules.vendor.domain.models.BadgeTier;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;
import com.danasea.backend.modules.vendor.infrastructure.mapper.VendorMapper;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MappersAdversarialStressTest {

    private ServiceMapper serviceMapper;
    private CategoryMapper categoryMapper;
    private ServiceImageMapper serviceImageMapper;
    private VendorMapper vendorMapper;

    @Mock
    private JpaVendorRepository jpaVendorRepository;

    private VendorInternalService vendorInternalService;

    @BeforeEach
    void setUp() {
        serviceMapper = new ServiceMapper();
        categoryMapper = new CategoryMapper();
        serviceImageMapper = new ServiceImageMapper();
        vendorMapper = new VendorMapper();
        vendorInternalService = new VendorInternalService(jpaVendorRepository, vendorMapper);
    }

    @Test
    @DisplayName("ServiceMapper: Full domain to entity to domain lossless roundtrip")
    void testServiceMapper_FullDomain_LosslessRoundtrip() {
        UUID id = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Service original = Service.builder()
                .id(id)
                .createdAt(now)
                .updatedAt(now)
                .vendorId(vendorId)
                .categoryId(categoryId)
                .name("Dịch vụ lặn ngắm san hô Bán Đảo Sơn Trà")
                .nameEn("Scuba Diving Tour at Son Tra Peninsula")
                .slug("dich-vu-lan-ngam-san-ho-son-tra")
                .description("Mô tả: Bao gồm tàu cao tốc, HDV chuyên nghiệp & bữa trưa hải sản tươi ngon 100%.")
                .descriptionEn("Description: Includes speed boat, pro guide & fresh seafood.")
                .price(new BigDecimal("1850000.50"))
                .durationMinutes(180)
                .capacityPerSlot(12)
                .locationName("Bãi Rạng, Bán đảo Sơn Trà, TP. Đà Nẵng")
                .address("Đường Hoàng Sa, Thọ Quang, Sơn Trà, Đà Nẵng")
                .latitude(new BigDecimal("16.1234567"))
                .longitude(new BigDecimal("108.2789101"))
                .status(ServiceStatus.PUBLISHED)
                .rejectionReason("Không có lý do")
                .waiverContent("{\"liability_acknowledged\": true, \"swimming_proficiency\": \"HIGH\"}")
                .weatherSensitive(true)
                .minWindKmh(new BigDecimal("12.5"))
                .maxWaveM(new BigDecimal("1.8"))
                .avgRating(new BigDecimal("4.95"))
                .ratingCount(128)
                .viewCount(3540)
                .build();

        ServiceJpaEntity entity = serviceMapper.toEntity(original);
        assertNotNull(entity);

        // Verify entity fields
        assertEquals(original.getId(), entity.getId());
        assertEquals(original.getCreatedAt(), entity.getCreatedAt());
        assertEquals(original.getUpdatedAt(), entity.getUpdatedAt());
        assertEquals(original.getVendorId(), entity.getVendorId());
        assertEquals(original.getCategoryId(), entity.getCategoryId());
        assertEquals(original.getName(), entity.getName());
        assertEquals(original.getNameEn(), entity.getNameEn());
        assertEquals(original.getSlug(), entity.getSlug());
        assertEquals(original.getDescription(), entity.getDescription());
        assertEquals(original.getDescriptionEn(), entity.getDescriptionEn());
        assertEquals(original.getPrice(), entity.getPrice());
        assertEquals(original.getDurationMinutes(), entity.getDurationMinutes());
        assertEquals(original.getCapacityPerSlot(), entity.getCapacityPerSlot());
        assertEquals(original.getLocationName(), entity.getLocationName());
        assertEquals(original.getAddress(), entity.getAddress());
        assertEquals(original.getLatitude(), entity.getLatitude());
        assertEquals(original.getLongitude(), entity.getLongitude());
        assertEquals(original.getStatus(), entity.getStatus());
        assertEquals(original.getRejectionReason(), entity.getRejectionReason());
        assertEquals(original.getWaiverContent(), entity.getWaiverContent());
        assertEquals(original.getWeatherSensitive(), entity.getWeatherSensitive());
        assertEquals(original.getMinWindKmh(), entity.getMinWindKmh());
        assertEquals(original.getMaxWaveM(), entity.getMaxWaveM());
        assertEquals(original.getAvgRating(), entity.getAvgRating());
        assertEquals(original.getRatingCount(), entity.getRatingCount());
        assertEquals(original.getViewCount(), entity.getViewCount());

        // Roundtrip back to Domain
        Service roundtrip = serviceMapper.toDomain(entity);
        assertNotNull(roundtrip);

        // Verify all 25 fields in roundtrip match original
        assertEquals(original.getId(), roundtrip.getId());
        assertEquals(original.getCreatedAt(), roundtrip.getCreatedAt());
        assertEquals(original.getUpdatedAt(), roundtrip.getUpdatedAt());
        assertEquals(original.getVendorId(), roundtrip.getVendorId());
        assertEquals(original.getCategoryId(), roundtrip.getCategoryId());
        assertEquals(original.getName(), roundtrip.getName());
        assertEquals(original.getNameEn(), roundtrip.getNameEn());
        assertEquals(original.getSlug(), roundtrip.getSlug());
        assertEquals(original.getDescription(), roundtrip.getDescription());
        assertEquals(original.getDescriptionEn(), roundtrip.getDescriptionEn());
        assertEquals(original.getPrice(), roundtrip.getPrice());
        assertEquals(original.getDurationMinutes(), roundtrip.getDurationMinutes());
        assertEquals(original.getCapacityPerSlot(), roundtrip.getCapacityPerSlot());
        assertEquals(original.getLocationName(), roundtrip.getLocationName());
        assertEquals(original.getAddress(), roundtrip.getAddress());
        assertEquals(original.getLatitude(), roundtrip.getLatitude());
        assertEquals(original.getLongitude(), roundtrip.getLongitude());
        assertEquals(original.getStatus(), roundtrip.getStatus());
        assertEquals(original.getRejectionReason(), roundtrip.getRejectionReason());
        assertEquals(original.getWaiverContent(), roundtrip.getWaiverContent());
        assertEquals(original.getWeatherSensitive(), roundtrip.getWeatherSensitive());
        assertEquals(original.getMinWindKmh(), roundtrip.getMinWindKmh());
        assertEquals(original.getMaxWaveM(), roundtrip.getMaxWaveM());
        assertEquals(original.getAvgRating(), roundtrip.getAvgRating());
        assertEquals(original.getRatingCount(), roundtrip.getRatingCount());
        assertEquals(original.getViewCount(), roundtrip.getViewCount());
    }

    @Test
    @DisplayName("ServiceMapper: Null fields defaults applied safely")
    void testServiceMapper_NullFieldDefaults() {
        Service bareMinimum = Service.builder().name("Tên tối thiểu").build();

        ServiceJpaEntity entity = serviceMapper.toEntity(bareMinimum);
        assertNotNull(entity);

        // Assert defaults
        assertEquals(ServiceStatus.DRAFT, entity.getStatus());
        assertEquals(Boolean.FALSE, entity.getWeatherSensitive());
        assertEquals(BigDecimal.ZERO, entity.getAvgRating());
        assertEquals(0, entity.getRatingCount());
        assertEquals(0, entity.getViewCount());
        assertNull(entity.getMinWindKmh());
        assertNull(entity.getMaxWaveM());
        assertNull(entity.getRejectionReason());

        Service domain = serviceMapper.toDomain(entity);
        assertNotNull(domain);
        assertEquals(ServiceStatus.DRAFT, domain.getStatus());
        assertEquals(Boolean.FALSE, domain.getWeatherSensitive());
        assertEquals(BigDecimal.ZERO, domain.getAvgRating());
        assertEquals(0, domain.getRatingCount());
        assertEquals(0, domain.getViewCount());
    }

    @Test
    @DisplayName("ServiceMapper: All status enum values preserved correctly")
    void testServiceMapper_AllStatusesPreserved() {
        for (ServiceStatus status : ServiceStatus.values()) {
            Service domain = Service.builder().name("Test").status(status).build();
            ServiceJpaEntity entity = serviceMapper.toEntity(domain);
            assertEquals(status, entity.getStatus(), "Status should be preserved: " + status);
            Service mappedBack = serviceMapper.toDomain(entity);
            assertEquals(status, mappedBack.getStatus(), "Status in domain should match: " + status);
        }
    }

    @Test
    @DisplayName("ServiceMapper: weatherSensitive flag handling (true, false, null)")
    void testServiceMapper_WeatherSensitiveValues() {
        // True
        Service s1 = Service.builder().weatherSensitive(true).build();
        assertEquals(Boolean.TRUE, serviceMapper.toEntity(s1).getWeatherSensitive());

        // False
        Service s2 = Service.builder().weatherSensitive(false).build();
        assertEquals(Boolean.FALSE, serviceMapper.toEntity(s2).getWeatherSensitive());

        // Null -> defaults to false
        Service s3 = Service.builder().weatherSensitive(null).build();
        assertEquals(Boolean.FALSE, serviceMapper.toEntity(s3).getWeatherSensitive());
    }

    @Test
    @DisplayName("ServiceMapper: List operations with null elements filtered safely")
    void testServiceMapper_ListOperationsWithNullElements() {
        Service s1 = Service.builder().name("S1").build();
        Service s2 = Service.builder().name("S2").build();

        List<ServiceJpaEntity> entities = serviceMapper.toEntityList(Arrays.asList(s1, null, s2));
        assertEquals(2, entities.size());
        assertEquals("S1", entities.get(0).getName());
        assertEquals("S2", entities.get(1).getName());

        List<Service> domains = serviceMapper.toDomainList(Arrays.asList(entities.get(0), null, entities.get(1)));
        assertEquals(2, domains.size());
        assertEquals("S1", domains.get(0).getName());
        assertEquals("S2", domains.get(1).getName());
    }

    @Test
    @DisplayName("CategoryMapper: Lossless roundtrip and isActive default behavior")
    void testCategoryMapper_RoundtripAndDefaults() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();

        // 1. Full with active=false: must preserve false, NOT overwrite to true!
        Category inactiveCategory = Category.builder()
                .id(id)
                .name("Thể thao mạo hiểm")
                .nameEn("Extreme Sports")
                .slug("the-thao-mao-hiem")
                .parentId(parentId)
                .iconUrl("https://cdn.example.com/icons/extreme.png")
                .isActive(false)
                .build();

        CategoryJpaEntity entityInactive = categoryMapper.toEntity(inactiveCategory);
        assertNotNull(entityInactive);
        assertEquals(Boolean.FALSE, entityInactive.getIsActive(), "Explicit isActive=false must NOT be overwritten!");
        Category mappedBackInactive = categoryMapper.toDomain(entityInactive);
        assertEquals(Boolean.FALSE, mappedBackInactive.getIsActive());

        // 2. Default when isActive is null: must default to TRUE
        Category defaultCategory = Category.builder().name("Lặn biển").build();
        CategoryJpaEntity entityDefault = categoryMapper.toEntity(defaultCategory);
        assertEquals(Boolean.TRUE, entityDefault.getIsActive(), "Null isActive must default to TRUE");

        // 3. List with null element
        List<CategoryJpaEntity> entities = categoryMapper.toEntityList(Arrays.asList(inactiveCategory, null));
        assertEquals(1, entities.size());
    }

    @Test
    @DisplayName("ServiceImageMapper: Lossless roundtrip and sortOrder default behavior")
    void testServiceImageMapper_RoundtripAndDefaults() {
        UUID id = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        // 1. Full with sortOrder=5
        ServiceImage img = ServiceImage.builder()
                .id(id)
                .serviceId(serviceId)
                .url("https://cdn.danasea.com/images/son-tra-1.jpg")
                .sortOrder((short) 5)
                .build();

        ServiceImageJpaEntity entity = serviceImageMapper.toEntity(img);
        assertNotNull(entity);
        assertEquals((short) 5, entity.getSortOrder());
        ServiceImage roundtrip = serviceImageMapper.toDomain(entity);
        assertEquals(id, roundtrip.getId());
        assertEquals(serviceId, roundtrip.getServiceId());
        assertEquals("https://cdn.danasea.com/images/son-tra-1.jpg", roundtrip.getUrl());
        assertEquals((short) 5, roundtrip.getSortOrder());

        // 2. Null sortOrder defaults to 0
        ServiceImage imgNoOrder = ServiceImage.builder().serviceId(serviceId).url("https://cdn.danasea.com/images/2.jpg").build();
        ServiceImageJpaEntity entityNoOrder = serviceImageMapper.toEntity(imgNoOrder);
        assertEquals((short) 0, entityNoOrder.getSortOrder());

        // 3. List with null element
        List<ServiceImageJpaEntity> entities = serviceImageMapper.toEntityList(Arrays.asList(img, null));
        assertEquals(1, entities.size());
    }

    @Test
    @DisplayName("VendorMapper: Full roundtrip with Vietnamese names and null safety")
    void testVendorMapper_FullRoundtrip() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID verifiedBy = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setUserId(userId);
        vendor.setBusinessName("Công ty TNHH Dịch Vụ Du Thuyền Đà Nẵng");
        vendor.setTaxCode("0401234567");
        vendor.setAddress("Số 12 Bạch Đằng, Quận Hải Châu, TP. Đà Nẵng");
        vendor.setBankAccountNumber("1029384756");
        vendor.setBankName("Ngân hàng TMCP Ngoại Thương Việt Nam (Vietcombank)");
        vendor.setBankAccountHolder("NGUYỄN VĂN AN");
        vendor.setVerificationStatus(VerificationStatus.APPROVED);
        vendor.setVerifiedBy(verifiedBy);
        vendor.setVerifiedAt(now);
        vendor.setRatingAvg(new BigDecimal("4.85"));
        vendor.setRatingCount(42);
        vendor.setBadgeTier(BadgeTier.TOP_RATED);

        VendorJpaEntity entity = vendorMapper.toEntity(vendor);
        assertNotNull(entity);
        assertEquals(vendor.getBusinessName(), entity.getBusinessName());
        assertEquals(vendor.getTaxCode(), entity.getTaxCode());
        assertEquals(vendor.getBankAccountHolder(), entity.getBankAccountHolder());
        assertEquals(vendor.getVerificationStatus(), entity.getVerificationStatus());
        assertEquals(vendor.getBadgeTier(), entity.getBadgeTier());

        Vendor roundtrip = vendorMapper.toDomain(entity);
        assertNotNull(roundtrip);
        assertEquals(vendor.getId(), roundtrip.getId());
        assertEquals(vendor.getUserId(), roundtrip.getUserId());
        assertEquals(vendor.getBusinessName(), roundtrip.getBusinessName());
        assertEquals(vendor.getTaxCode(), roundtrip.getTaxCode());
        assertEquals(vendor.getAddress(), roundtrip.getAddress());
        assertEquals(vendor.getBankAccountNumber(), roundtrip.getBankAccountNumber());
        assertEquals(vendor.getBankName(), roundtrip.getBankName());
        assertEquals(vendor.getBankAccountHolder(), roundtrip.getBankAccountHolder());
        assertEquals(vendor.getVerificationStatus(), roundtrip.getVerificationStatus());
        assertEquals(vendor.getVerifiedBy(), roundtrip.getVerifiedBy());
        assertEquals(vendor.getVerifiedAt(), roundtrip.getVerifiedAt());
        assertEquals(vendor.getRatingAvg(), roundtrip.getRatingAvg());
        assertEquals(vendor.getRatingCount(), roundtrip.getRatingCount());
        assertEquals(vendor.getBadgeTier(), roundtrip.getBadgeTier());
    }

    @Test
    @DisplayName("VendorInternalService: Adversarial boundary checks (null userId, null vendorId, pending/rejected status)")
    void testVendorInternalService_AdversarialChecks() {
        UUID vendorId = UUID.randomUUID();

        // 1. null vendorId -> returns false
        assertFalse(vendorInternalService.isVendorApproved(null));

        // 2. null userId -> returns empty
        assertTrue(vendorInternalService.findByUserId(null).isEmpty());

        // 3. null vendorId -> returns empty
        assertTrue(vendorInternalService.findById(null).isEmpty());

        // 4. vendor not found in DB
        when(jpaVendorRepository.findById(vendorId)).thenReturn(Optional.empty());
        assertFalse(vendorInternalService.isVendorApproved(vendorId));

        // 5. vendor is PENDING
        VendorJpaEntity pendingEntity = new VendorJpaEntity();
        pendingEntity.setId(vendorId);
        pendingEntity.setVerificationStatus(VerificationStatus.PENDING);
        when(jpaVendorRepository.findById(vendorId)).thenReturn(Optional.of(pendingEntity));
        assertFalse(vendorInternalService.isVendorApproved(vendorId));

        // 6. vendor is REJECTED
        VendorJpaEntity rejectedEntity = new VendorJpaEntity();
        rejectedEntity.setId(vendorId);
        rejectedEntity.setVerificationStatus(VerificationStatus.REJECTED);
        when(jpaVendorRepository.findById(vendorId)).thenReturn(Optional.of(rejectedEntity));
        assertFalse(vendorInternalService.isVendorApproved(vendorId));

        // 7. vendor is APPROVED
        VendorJpaEntity approvedEntity = new VendorJpaEntity();
        approvedEntity.setId(vendorId);
        approvedEntity.setVerificationStatus(VerificationStatus.APPROVED);
        when(jpaVendorRepository.findById(vendorId)).thenReturn(Optional.of(approvedEntity));
        assertTrue(vendorInternalService.isVendorApproved(vendorId));
    }
}
