package com.danasea.backend.modules.service.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.service.application.dtos.ServiceResult;
import com.danasea.backend.modules.service.application.dtos.UpdateServiceCommand;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateServiceUseCase Tests")
class UpdateServiceUseCaseTest {

    @Mock private ServiceRepositoryPort serviceRepository;
    @Mock private CategoryRepositoryPort categoryRepository;
    @Mock private ServiceImageRepositoryPort serviceImageRepository;
    @Mock private VendorPort vendorPort;

    private UpdateServiceUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();
    private final UUID otherVendorId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new UpdateServiceUseCase(serviceRepository, categoryRepository, serviceImageRepository, vendorPort);
    }

    private Vendor vendor(UUID id) {
        Vendor v = new Vendor();
        v.setId(id);
        v.setUserId(userId);
        v.setVerificationStatus(VerificationStatus.APPROVED);
        return v;
    }

    private Service serviceWithStatus(UUID ownerId, ServiceStatus status) {
        return Service.builder()
                .id(serviceId)
                .vendorId(ownerId)
                .name("Old Name")
                .status(status)
                .build();
    }

    private UpdateServiceCommand cmd(String name) {
        return new UpdateServiceCommand(
                userId, serviceId, null, name, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null
        );
    }

    @Test
    @DisplayName("✅ Sửa khi DRAFT → thành công, status giữ nguyên DRAFT")
    void update_draft_success() {
        Service draft = serviceWithStatus(vendorId, ServiceStatus.DRAFT);
        Service saved = serviceWithStatus(vendorId, ServiceStatus.DRAFT);
        saved.setName("New Name");

        when(vendorPort.findByUserId(userId)).thenReturn(Optional.of(vendor(vendorId)));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(draft));
        when(serviceRepository.save(any())).thenReturn(saved);
        when(serviceImageRepository.findByServiceId(any())).thenReturn(List.of());

        ServiceResult result = useCase.execute(cmd("New Name"));

        assertThat(result.status()).isEqualTo(ServiceStatus.DRAFT);
        verify(serviceRepository).save(any());
    }

    @Test
    @DisplayName("✅ Sửa khi PUBLISHED → tự động chuyển về PENDING_REVIEW")
    void update_published_transitionsToPendingReview() {
        Service published = serviceWithStatus(vendorId, ServiceStatus.PUBLISHED);
        Service saved = serviceWithStatus(vendorId, ServiceStatus.PENDING_REVIEW);

        when(vendorPort.findByUserId(userId)).thenReturn(Optional.of(vendor(vendorId)));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(published));
        when(serviceRepository.save(any())).thenAnswer(inv -> {
            Service s = inv.getArgument(0);
            assertThat(s.getStatus()).isEqualTo(ServiceStatus.PENDING_REVIEW);
            return saved;
        });
        when(serviceImageRepository.findByServiceId(any())).thenReturn(List.of());

        ServiceResult result = useCase.execute(cmd("Updated Name"));

        assertThat(result.status()).isEqualTo(ServiceStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("❌ Vendor A sửa dịch vụ của Vendor B → UnauthorizedServiceAccessException (403)")
    void update_wrongVendor_throwsForbidden() {
        // Service belongs to otherVendorId, but user is vendor
        Service serviceOfOther = serviceWithStatus(otherVendorId, ServiceStatus.DRAFT);

        when(vendorPort.findByUserId(userId)).thenReturn(Optional.of(vendor(vendorId)));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceOfOther));

        assertThatThrownBy(() -> useCase.execute(cmd("Hacked Name")))
                .isInstanceOf(UnauthorizedServiceAccessException.class);
    }
}
