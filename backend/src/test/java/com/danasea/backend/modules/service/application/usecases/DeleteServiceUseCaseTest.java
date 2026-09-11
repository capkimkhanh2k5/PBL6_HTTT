package com.danasea.backend.modules.service.application.usecases;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.service.domain.exceptions.InvalidServiceStateException;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteServiceUseCase Tests")
class DeleteServiceUseCaseTest {

    @Mock private ServiceRepositoryPort serviceRepository;
    @Mock private ServiceImageRepositoryPort serviceImageRepository;
    @Mock private VendorPort vendorPort;

    private DeleteServiceUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new DeleteServiceUseCase(serviceRepository, serviceImageRepository, vendorPort);
    }

    private Vendor vendor() {
        Vendor v = new Vendor();
        v.setId(vendorId);
        v.setUserId(userId);
        v.setVerificationStatus(VerificationStatus.APPROVED);
        return v;
    }

    private Service serviceWithStatus(UUID ownerId, ServiceStatus status) {
        return Service.builder().id(serviceId).vendorId(ownerId).status(status).build();
    }

    @Test
    @DisplayName("✅ Xóa khi DRAFT → thành công")
    void delete_draft_success() {
        Service draft = serviceWithStatus(vendorId, ServiceStatus.DRAFT);

        when(vendorPort.findByUserId(userId)).thenReturn(Optional.of(vendor()));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(draft));

        assertThatCode(() -> useCase.execute(userId, serviceId)).doesNotThrowAnyException();

        verify(serviceImageRepository).deleteByServiceId(serviceId);
        verify(serviceRepository).deleteById(serviceId);
    }

    @Test
    @DisplayName("❌ Xóa khi PUBLISHED → InvalidServiceStateException với message rõ ràng")
    void delete_published_throwsWithClearMessage() {
        Service published = serviceWithStatus(vendorId, ServiceStatus.PUBLISHED);

        when(vendorPort.findByUserId(userId)).thenReturn(Optional.of(vendor()));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> useCase.execute(userId, serviceId))
                .isInstanceOf(InvalidServiceStateException.class)
                .hasMessageContaining("DRAFT");

        verify(serviceRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("❌ Xóa khi PAUSED → InvalidServiceStateException")
    void delete_paused_throwsException() {
        Service paused = serviceWithStatus(vendorId, ServiceStatus.PAUSED);

        when(vendorPort.findByUserId(userId)).thenReturn(Optional.of(vendor()));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(paused));

        assertThatThrownBy(() -> useCase.execute(userId, serviceId))
                .isInstanceOf(InvalidServiceStateException.class);

        verify(serviceRepository, never()).deleteById(any());
    }
}
