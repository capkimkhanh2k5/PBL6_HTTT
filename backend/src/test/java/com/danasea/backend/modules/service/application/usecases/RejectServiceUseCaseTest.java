package com.danasea.backend.modules.service.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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

import com.danasea.backend.modules.service.application.dto.RejectServiceCommand;
import com.danasea.backend.modules.service.application.dto.ServiceResult;
import com.danasea.backend.modules.service.domain.exceptions.InvalidServiceStateException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.AuditLogPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("RejectServiceUseCase Tests")
class RejectServiceUseCaseTest {

    @Mock private ServiceRepositoryPort serviceRepository;
    @Mock private ServiceImageRepositoryPort serviceImageRepository;
    @Mock private AuditLogPort auditLogPort;

    private RejectServiceUseCase useCase;

    private final UUID adminUserId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new RejectServiceUseCase(serviceRepository, serviceImageRepository, auditLogPort);
    }

    private Service serviceWithStatus(ServiceStatus status) {
        return Service.builder().id(serviceId).vendorId(UUID.randomUUID()).status(status).build();
    }

    @Test
    @DisplayName("✅ Reject từ PENDING_REVIEW → REJECTED, lưu reason, ghi audit log SERVICE_REJECTED")
    void reject_pendingReview_rejectedWithReasonAndAuditLog() {
        String reason = "Photos are not clear";
        Service pending = serviceWithStatus(ServiceStatus.PENDING_REVIEW);
        Service saved = Service.builder()
                .id(serviceId)
                .vendorId(UUID.randomUUID())
                .status(ServiceStatus.REJECTED)
                .rejectionReason(reason)
                .build();

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(pending));
        when(serviceRepository.save(any())).thenReturn(saved);
        when(serviceImageRepository.findByServiceId(any())).thenReturn(List.of());

        RejectServiceCommand cmd = new RejectServiceCommand(adminUserId, serviceId, reason);
        ServiceResult result = useCase.execute(cmd);

        assertThat(result.status()).isEqualTo(ServiceStatus.REJECTED);
        assertThat(result.rejectionReason()).isEqualTo(reason);

        verify(auditLogPort).recordAuditLog(
                eq(adminUserId), eq("SERVICE_REJECTED"), eq("SERVICE"), eq(serviceId),
                contains(reason)
        );
    }

    @Test
    @DisplayName("❌ Reject dịch vụ không ở trạng thái PENDING_REVIEW → InvalidServiceStateException")
    void reject_notPendingReview_throwsException() {
        Service draft = serviceWithStatus(ServiceStatus.DRAFT);
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(draft));

        RejectServiceCommand cmd = new RejectServiceCommand(adminUserId, serviceId, "Some reason");

        assertThatThrownBy(() -> useCase.execute(cmd))
                .isInstanceOf(InvalidServiceStateException.class);

        verify(auditLogPort, never()).recordAuditLog(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("❌ Reject mà không có reason → IllegalArgumentException")
    void reject_blankReason_throwsException() {
        Service pending = serviceWithStatus(ServiceStatus.PENDING_REVIEW);
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(pending));

        RejectServiceCommand cmd = new RejectServiceCommand(adminUserId, serviceId, "");

        assertThatThrownBy(() -> useCase.execute(cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
