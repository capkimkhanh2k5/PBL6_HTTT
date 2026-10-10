package com.danasea.backend.modules.service.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.danasea.backend.modules.service.application.dtos.CreateServiceCommand;
import com.danasea.backend.modules.service.application.dtos.ServiceResult;
import com.danasea.backend.modules.service.application.dtos.UpdateServiceCommand;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.domain.exceptions.WaiverContentRequiredException;
import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("Vendor Service Waiver Policy Tests")
class VendorServiceWaiverPolicyTest {

    @Mock private ServiceRepositoryPort serviceRepository;
    @Mock private CategoryRepositoryPort categoryRepository;
    @Mock private ServiceImageRepositoryPort serviceImageRepository;
    @Mock private VendorPort vendorPort;

    private CreateServiceUseCase createServiceUseCase;
    private UpdateServiceUseCase updateServiceUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();
    private final UUID otherVendorId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createServiceUseCase =
                new CreateServiceUseCase(
                        serviceRepository, categoryRepository, serviceImageRepository, vendorPort);
        updateServiceUseCase =
                new UpdateServiceUseCase(
                        serviceRepository, categoryRepository, serviceImageRepository, vendorPort);
    }

    private Vendor approvedVendor(UUID id, UUID uid) {
        Vendor v = new Vendor();
        v.setId(id);
        v.setUserId(uid);
        v.setVerificationStatus(VerificationStatus.APPROVED);
        return v;
    }

    private Category activeCategory() {
        Category c = new Category();
        c.setId(categoryId);
        c.setName("Tour");
        c.setIsActive(true);
        return c;
    }

    private CreateServiceCommand createCmd(
            String vi, String en, Boolean required, Boolean weatherSensitive) {
        return new CreateServiceCommand(
                userId,
                categoryId,
                "Lặn biển ngắm san hô",
                "Diving Tour",
                "Mô tả",
                "Desc",
                BigDecimal.valueOf(500000),
                60,
                10,
                "Bán đảo Sơn Trà",
                "Đà Nẵng",
                BigDecimal.valueOf(16.1),
                BigDecimal.valueOf(108.2),
                vi,
                en,
                required,
                weatherSensitive,
                BigDecimal.valueOf(25),
                BigDecimal.valueOf(1.5));
    }

    private UpdateServiceCommand updateCmd(
            UUID uid, String name, BigDecimal price, String vi, String en, Boolean required) {
        return new UpdateServiceCommand(
                uid, serviceId, null, name, null, null, null, price, null, null, null, null, null,
                null, vi, en, required, null, null, null);
    }

    @Nested
    @DisplayName("CreateService - Chính sách khởi tạo cam kết")
    class CreateServicePolicyTests {

        @Test
        @DisplayName("Thành công: waiverRequired=true kèm nội dung hợp lệ khởi tạo waiverVersion=1")
        void createWithWaiverSuccess() {
            when(vendorPort.findByUserId(userId))
                    .thenReturn(Optional.of(approvedVendor(vendorId, userId)));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory()));
            when(serviceRepository.save(any(Service.class)))
                    .thenAnswer(
                            inv -> {
                                Service s = inv.getArgument(0);
                                s.setId(serviceId);
                                return s;
                            });

            CreateServiceCommand cmd =
                    createCmd("Cam kết an toàn khi lặn", "Safety waiver when diving", true, true);

            ServiceResult result = createServiceUseCase.execute(cmd);

            assertThat(result.waiverRequired()).isTrue();
            assertThat(result.waiverVersion()).isEqualTo(1);
            assertThat(result.waiverContent()).isEqualTo("Cam kết an toàn khi lặn");
            assertThat(result.waiverContentEn()).isEqualTo("Safety waiver when diving");
        }

        @Test
        @DisplayName(
                "Lỗi: waiverRequired=true nhưng nội dung trống ném WaiverContentRequiredException")
        void createWithWaiverRequiredAndEmptyContentThrows() {
            when(vendorPort.findByUserId(userId))
                    .thenReturn(Optional.of(approvedVendor(vendorId, userId)));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory()));

            CreateServiceCommand cmd = createCmd("   ", "", true, false);

            assertThatThrownBy(() -> createServiceUseCase.execute(cmd))
                    .isInstanceOf(WaiverContentRequiredException.class);
        }

        @Test
        @DisplayName("weatherSensitive=true KHÔNG tự suy ra waiverRequired=true")
        void weatherSensitiveDoesNotImplyWaiverRequired() {
            when(vendorPort.findByUserId(userId))
                    .thenReturn(Optional.of(approvedVendor(vendorId, userId)));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(activeCategory()));
            when(serviceRepository.save(any(Service.class)))
                    .thenAnswer(
                            inv -> {
                                Service s = inv.getArgument(0);
                                s.setId(serviceId);
                                return s;
                            });

            CreateServiceCommand cmd = createCmd(null, null, false, true);

            ServiceResult result = createServiceUseCase.execute(cmd);

            assertThat(result.weatherSensitive()).isTrue();
            assertThat(result.waiverRequired()).isFalse();
            assertThat(result.waiverVersion()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("UpdateService - Chính sách quản lý version & dirty check")
    class UpdateServicePolicyTests {

        private Service existingService(
                UUID ownerId, boolean required, int version, String vi, String en) {
            return Service.builder()
                    .id(serviceId)
                    .vendorId(ownerId)
                    .name("Dịch vụ gốc")
                    .price(BigDecimal.valueOf(100000))
                    .status(ServiceStatus.PUBLISHED)
                    .waiverRequired(required)
                    .waiverVersion(version)
                    .waiverContent(vi)
                    .waiverContentEn(en)
                    .build();
        }

        @Test
        @DisplayName("Vendor khác không được sửa dịch vụ của người khác")
        void otherVendorCannotUpdateService() {
            when(vendorPort.findByUserId(otherUserId))
                    .thenReturn(Optional.of(approvedVendor(otherVendorId, otherUserId)));
            when(serviceRepository.findByIdForUpdate(serviceId))
                    .thenReturn(Optional.of(existingService(vendorId, true, 1, "VI", "EN")));

            UpdateServiceCommand cmd = updateCmd(otherUserId, "Tên mới", null, null, null, null);

            assertThatThrownBy(() -> updateServiceUseCase.execute(cmd))
                    .isInstanceOf(UnauthorizedServiceAccessException.class);
        }

        @Test
        @DisplayName("Đổi giá/tên mà không đổi cam kết thì waiverVersion GIỮ NGUYÊN")
        void updatePriceDoesNotIncrementWaiverVersion() {
            Service existing = existingService(vendorId, true, 1, "Cam kết cũ", "Old waiver");
            when(vendorPort.findByUserId(userId))
                    .thenReturn(Optional.of(approvedVendor(vendorId, userId)));
            when(serviceRepository.findByIdForUpdate(serviceId)).thenReturn(Optional.of(existing));
            when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateServiceCommand cmd =
                    updateCmd(
                            userId,
                            "Tên mới",
                            BigDecimal.valueOf(200000),
                            "  Cam kết cũ  ",
                            "Old waiver",
                            true);

            ServiceResult result = updateServiceUseCase.execute(cmd);

            assertThat(result.waiverVersion()).isEqualTo(1);
            assertThat(result.price()).isEqualByComparingTo(BigDecimal.valueOf(200000));
        }

        @Test
        @DisplayName("Thay đổi nội dung cam kết thì waiverVersion TĂNG 1")
        void updateWaiverContentIncrementsVersion() {
            Service existing = existingService(vendorId, true, 1, "Cam kết cũ", "Old waiver");
            when(vendorPort.findByUserId(userId))
                    .thenReturn(Optional.of(approvedVendor(vendorId, userId)));
            when(serviceRepository.findByIdForUpdate(serviceId)).thenReturn(Optional.of(existing));
            when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateServiceCommand cmd =
                    updateCmd(
                            userId,
                            null,
                            null,
                            "Cam kết mới đã cập nhật",
                            "Updated new waiver",
                            true);

            ServiceResult result = updateServiceUseCase.execute(cmd);

            assertThat(result.waiverVersion()).isEqualTo(2);
            assertThat(result.waiverContent()).isEqualTo("Cam kết mới đã cập nhật");
            assertThat(result.waiverContentEn()).isEqualTo("Updated new waiver");
        }

        @Test
        @DisplayName("Bật waiverRequired từ false sang true thì waiverVersion TĂNG 1")
        void enablingWaiverRequiredIncrementsVersion() {
            Service existing =
                    existingService(vendorId, false, 1, "Cam kết có sẵn", "Existing waiver");
            when(vendorPort.findByUserId(userId))
                    .thenReturn(Optional.of(approvedVendor(vendorId, userId)));
            when(serviceRepository.findByIdForUpdate(serviceId)).thenReturn(Optional.of(existing));
            when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateServiceCommand cmd =
                    updateCmd(userId, null, null, "Cam kết có sẵn", "Existing waiver", true);

            ServiceResult result = updateServiceUseCase.execute(cmd);

            assertThat(result.waiverRequired()).isTrue();
            assertThat(result.waiverVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("Lỗi: cập nhật waiverRequired=true nhưng nội dung cả VI và EN đều trống")
        void updateWithRequiredTrueAndBlankContentThrows() {
            Service existing = existingService(vendorId, false, 1, null, null);
            when(vendorPort.findByUserId(userId))
                    .thenReturn(Optional.of(approvedVendor(vendorId, userId)));
            when(serviceRepository.findByIdForUpdate(serviceId)).thenReturn(Optional.of(existing));

            UpdateServiceCommand cmd = updateCmd(userId, null, null, "   ", "", true);

            assertThatThrownBy(() -> updateServiceUseCase.execute(cmd))
                    .isInstanceOf(WaiverContentRequiredException.class);
        }
    }
}
