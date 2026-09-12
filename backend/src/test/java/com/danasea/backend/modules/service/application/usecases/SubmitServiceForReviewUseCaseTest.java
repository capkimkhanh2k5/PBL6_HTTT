package com.danasea.backend.modules.service.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.danasea.backend.modules.service.domain.exceptions.InvalidServiceStateException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceImagesRequiredException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmitServiceForReviewUseCase Tests")
class SubmitServiceForReviewUseCaseTest {

    @Mock private ServiceRepositoryPort serviceRepository;
    @Mock private ServiceImageRepositoryPort serviceImageRepository;
    @Mock private VendorPort vendorPort;

    private SubmitServiceForReviewUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new SubmitServiceForReviewUseCase(serviceRepository, serviceImageRepository, vendorPort);
    }

    private Vendor vendor() {
        Vendor v = new Vendor();
        v.setId(vendorId);
        v.setUserId(userId);
        v.setVerificationStatus(VerificationStatus.APPROVED);
        return v;
    }

    private Service serviceWithStatus(ServiceStatus status) {
        Service s = Service.builder()
                .id(serviceId)
                .vendorId(vendorId)
                .status(status)
                .build();
        return s;
    }

    @Test
    @DisplayName("✅ DRAFT → PENDING_REVIEW thành công khi có ảnh")
    void submit_draftWithImages_success() {
        Service draft = serviceWithStatus(ServiceStatus.DRAFT);
        Service saved = serviceWithStatus(ServiceStatus.PENDING_REVIEW);

        when(vendorPort.findByUserId(userId)).thenReturn(Optional.of(vendor()));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(draft));
        when(serviceImageRepository.existsByServiceId(serviceId)).thenReturn(true);
        when(serviceRepository.save(any())).thenReturn(saved);
        when(serviceImageRepository.findByServiceId(any())).thenReturn(List.of());

        ServiceResult result = useCase.execute(userId, serviceId);

        assertThat(result.status()).isEqualTo(ServiceStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("✅ REJECTED → PENDING_REVIEW thành công (submit lại sau khi bị từ chối)")
    void submit_rejectedWithImages_success() {
        Service rejected = serviceWithStatus(ServiceStatus.REJECTED);
        Service saved = serviceWithStatus(ServiceStatus.PENDING_REVIEW);

        when(vendorPort.findByUserId(userId)).thenReturn(Optional.of(vendor()));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(rejected));
        when(serviceImageRepository.existsByServiceId(serviceId)).thenReturn(true);
        when(serviceRepository.save(any())).thenReturn(saved);
        when(serviceImageRepository.findByServiceId(any())).thenReturn(List.of());

        ServiceResult result = useCase.execute(userId, serviceId);

        assertThat(result.status()).isEqualTo(ServiceStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("❌ Thiếu ảnh (service_images rỗng) → ServiceImagesRequiredException")
    void submit_noImages_throwsException() {
        Service draft = serviceWithStatus(ServiceStatus.DRAFT);

        when(vendorPort.findByUserId(userId)).thenReturn(Optional.of(vendor()));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(draft));
        when(serviceImageRepository.existsByServiceId(serviceId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(userId, serviceId))
                .isInstanceOf(ServiceImagesRequiredException.class);

        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Submit khi đang PUBLISHED (status không hợp lệ) → InvalidServiceStateException")
    void submit_publishedStatus_throwsException() {
        Service published = serviceWithStatus(ServiceStatus.PUBLISHED);

        when(vendorPort.findByUserId(userId)).thenReturn(Optional.of(vendor()));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(published));
        when(serviceImageRepository.existsByServiceId(serviceId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(userId, serviceId))
                .isInstanceOf(InvalidServiceStateException.class);
    }
}
