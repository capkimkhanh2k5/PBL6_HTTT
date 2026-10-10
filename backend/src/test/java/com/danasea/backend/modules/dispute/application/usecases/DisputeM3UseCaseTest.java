package com.danasea.backend.modules.dispute.application.usecases;

import com.danasea.backend.modules.dispute.domain.exceptions.DisputeAlreadyResolvedException;
import com.danasea.backend.modules.dispute.domain.exceptions.DisputeNotFoundException;
import com.danasea.backend.modules.dispute.domain.exceptions.UnauthorizedDisputeAccessException;
import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.dispute.infrastructure.persistence.entities.DisputeJpaEntity;
import com.danasea.backend.modules.dispute.infrastructure.persistence.repositories.JpaDisputeRepository;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import com.danasea.backend.modules.dispute.presentation.dtos.EvidenceUploadResponse;
import com.danasea.backend.modules.dispute.presentation.dtos.SubmitVendorResponseRequest;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.application.ports.FileStoragePort;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dispute Milestone 3 Use Cases Unit Tests")
class DisputeM3UseCaseTest {

    @Mock
    private JpaDisputeRepository disputeRepository;

    @Mock
    private JpaSubOrderRepository subOrderRepository;

    @Mock
    private VendorInternalApi vendorInternalApi;

    @Mock
    private FileStoragePort fileStoragePort;

    private UUID customerId;
    private UUID vendorUserId;
    private UUID vendorId;
    private UUID orderId;
    private UUID subOrderId;
    private UUID disputeId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        vendorUserId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        subOrderId = UUID.randomUUID();
        disputeId = UUID.randomUUID();
    }

    private Vendor createVendor(UUID id, UUID userId) {
        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setUserId(userId);
        return vendor;
    }

    @Nested
    @DisplayName("GetCustomerDisputesUseCase Tests")
    class GetCustomerDisputesUseCaseTests {

        @Test
        @DisplayName("Returns paged customer disputes")
        void execute_Success() {
            GetCustomerDisputesUseCase useCase = new GetCustomerDisputesUseCase(disputeRepository);
            Pageable pageable = PageRequest.of(0, 10);

            DisputeJpaEntity entity = DisputeJpaEntity.builder()
                    .orderId(orderId)
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .reason(DisputeReason.SERVICE_NOT_AS_DESCRIBED)
                    .description("Test issue")
                    .status(DisputeStatus.OPEN)
                    .build();
            entity.setId(disputeId);

            when(disputeRepository.findByCustomerId(customerId, pageable))
                    .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

            Page<DisputeResponse> result = useCase.execute(pageable, customerId);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(disputeId);
            assertThat(result.getContent().get(0).customerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when pageable is invalid")
        void execute_InvalidPageable() {
            GetCustomerDisputesUseCase useCase = new GetCustomerDisputesUseCase(disputeRepository);
            assertThatThrownBy(() -> useCase.execute(PageRequest.of(-1, 10), customerId))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("GetCustomerDisputeDetailUseCase Tests")
    class GetCustomerDisputeDetailUseCaseTests {

        @Test
        @DisplayName("Returns dispute detail when customer matches")
        void execute_Success() {
            GetCustomerDisputeDetailUseCase useCase = new GetCustomerDisputeDetailUseCase(disputeRepository);

            DisputeJpaEntity entity = DisputeJpaEntity.builder()
                    .orderId(orderId)
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .reason(DisputeReason.SERVICE_NOT_AS_DESCRIBED)
                    .status(DisputeStatus.OPEN)
                    .build();
            entity.setId(disputeId);

            when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(entity));

            DisputeResponse response = useCase.execute(disputeId, customerId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(disputeId);
            assertThat(response.customerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("Throws UnauthorizedDisputeAccessException when customer does not match (IDOR)")
        void execute_CustomerIsolationViolation() {
            GetCustomerDisputeDetailUseCase useCase = new GetCustomerDisputeDetailUseCase(disputeRepository);

            UUID anotherCustomerId = UUID.randomUUID();
            DisputeJpaEntity entity = DisputeJpaEntity.builder()
                    .orderId(orderId)
                    .subOrderId(subOrderId)
                    .customerId(anotherCustomerId)
                    .reason(DisputeReason.SERVICE_NOT_AS_DESCRIBED)
                    .status(DisputeStatus.OPEN)
                    .build();
            entity.setId(disputeId);

            when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> useCase.execute(disputeId, customerId))
                    .isInstanceOf(UnauthorizedDisputeAccessException.class);
        }

        @Test
        @DisplayName("Throws DisputeNotFoundException when dispute does not exist")
        void execute_NotFound() {
            GetCustomerDisputeDetailUseCase useCase = new GetCustomerDisputeDetailUseCase(disputeRepository);
            when(disputeRepository.findById(disputeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(disputeId, customerId))
                    .isInstanceOf(DisputeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("GetVendorDisputesUseCase Tests")
    class GetVendorDisputesUseCaseTests {

        @Test
        @DisplayName("Returns vendor disputes for sub-orders owned by vendor")
        void execute_Success() {
            GetVendorDisputesUseCase useCase = new GetVendorDisputesUseCase(
                    disputeRepository, subOrderRepository, vendorInternalApi);

            Vendor vendor = createVendor(vendorId, vendorUserId);
            when(vendorInternalApi.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(subOrderId);
            subOrder.setVendorId(vendorId);

            when(subOrderRepository.findByVendorId(vendorId)).thenReturn(List.of(subOrder));

            DisputeJpaEntity entity = DisputeJpaEntity.builder()
                    .orderId(orderId)
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .status(DisputeStatus.OPEN)
                    .build();
            entity.setId(disputeId);

            Pageable pageable = PageRequest.of(0, 10);
            when(disputeRepository.findBySubOrderIdInAndStatus(List.of(subOrderId), DisputeStatus.OPEN, pageable))
                    .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

            Page<DisputeResponse> result = useCase.execute(DisputeStatus.OPEN, pageable, vendorUserId);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(disputeId);
        }

        @Test
        @DisplayName("Returns empty page when vendor has no sub-orders")
        void execute_VendorHasNoSubOrders_ReturnsEmptyPage() {
            GetVendorDisputesUseCase useCase = new GetVendorDisputesUseCase(
                    disputeRepository, subOrderRepository, vendorInternalApi);

            Vendor vendor = createVendor(vendorId, vendorUserId);
            when(vendorInternalApi.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));
            when(subOrderRepository.findByVendorId(vendorId)).thenReturn(List.of());

            Pageable pageable = PageRequest.of(0, 10);
            Page<DisputeResponse> result = useCase.execute(null, pageable, vendorUserId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("GetVendorDisputeDetailUseCase Tests")
    class GetVendorDisputeDetailUseCaseTests {

        @Test
        @DisplayName("Returns dispute detail when vendor owns the sub-order")
        void execute_Success() {
            GetVendorDisputeDetailUseCase useCase = new GetVendorDisputeDetailUseCase(
                    disputeRepository, subOrderRepository, vendorInternalApi);

            Vendor vendor = createVendor(vendorId, vendorUserId);
            when(vendorInternalApi.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));

            DisputeJpaEntity dispute = DisputeJpaEntity.builder()
                    .orderId(orderId)
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .status(DisputeStatus.OPEN)
                    .build();
            dispute.setId(disputeId);

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(subOrderId);
            subOrder.setVendorId(vendorId);

            when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
            when(subOrderRepository.findById(subOrderId)).thenReturn(Optional.of(subOrder));

            DisputeResponse response = useCase.execute(disputeId, vendorUserId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(disputeId);
            assertThat(response.subOrderId()).isEqualTo(subOrderId);
        }

        @Test
        @DisplayName("Throws UnauthorizedDisputeAccessException when vendor does not own sub-order")
        void execute_VendorIsolationViolation() {
            GetVendorDisputeDetailUseCase useCase = new GetVendorDisputeDetailUseCase(
                    disputeRepository, subOrderRepository, vendorInternalApi);

            Vendor vendor = createVendor(vendorId, vendorUserId);
            when(vendorInternalApi.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));

            DisputeJpaEntity dispute = DisputeJpaEntity.builder()
                    .orderId(orderId)
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .status(DisputeStatus.OPEN)
                    .build();
            dispute.setId(disputeId);

            UUID otherVendorId = UUID.randomUUID();
            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(subOrderId);
            subOrder.setVendorId(otherVendorId);

            when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
            when(subOrderRepository.findById(subOrderId)).thenReturn(Optional.of(subOrder));

            assertThatThrownBy(() -> useCase.execute(disputeId, vendorUserId))
                    .isInstanceOf(UnauthorizedDisputeAccessException.class);
        }
    }

    @Nested
    @DisplayName("SubmitVendorDisputeResponseUseCase Tests")
    class SubmitVendorDisputeResponseUseCaseTests {

        @Test
        @DisplayName("Successfully updates dispute with vendor explanation and transitions OPEN to UNDER_REVIEW")
        void execute_Success() {
            SubmitVendorDisputeResponseUseCase useCase = new SubmitVendorDisputeResponseUseCase(
                    disputeRepository, subOrderRepository, vendorInternalApi);

            Vendor vendor = createVendor(vendorId, vendorUserId);
            when(vendorInternalApi.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));

            DisputeJpaEntity dispute = DisputeJpaEntity.builder()
                    .orderId(orderId)
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .status(DisputeStatus.OPEN)
                    .build();
            dispute.setId(disputeId);

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(subOrderId);
            subOrder.setVendorId(vendorId);

            when(disputeRepository.findByIdForUpdate(disputeId)).thenReturn(Optional.of(dispute));
            when(subOrderRepository.findById(subOrderId)).thenReturn(Optional.of(subOrder));
            when(disputeRepository.save(any(DisputeJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            SubmitVendorResponseRequest request = new SubmitVendorResponseRequest(
                    "Service was delivered according to contract.",
                    List.of("http://example.com/log.pdf")
            );

            DisputeResponse response = useCase.execute(disputeId, request, vendorUserId);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(DisputeStatus.UNDER_REVIEW);
            assertThat(response.vendorResponse()).isEqualTo("Service was delivered according to contract.");
            assertThat(response.vendorEvidenceUrls()).containsExactly("http://example.com/log.pdf");
            assertThat(response.vendorRespondedAt()).isNotNull();

            ArgumentCaptor<DisputeJpaEntity> captor = ArgumentCaptor.forClass(DisputeJpaEntity.class);
            verify(disputeRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(DisputeStatus.UNDER_REVIEW);
            assertThat(captor.getValue().getVendorResponse()).isEqualTo("Service was delivered according to contract.");
        }

        @Test
        @DisplayName("Throws DisputeAlreadyResolvedException when dispute is already resolved")
        void execute_AlreadyResolved() {
            SubmitVendorDisputeResponseUseCase useCase = new SubmitVendorDisputeResponseUseCase(
                    disputeRepository, subOrderRepository, vendorInternalApi);

            Vendor vendor = createVendor(vendorId, vendorUserId);
            when(vendorInternalApi.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));

            DisputeJpaEntity dispute = DisputeJpaEntity.builder()
                    .orderId(orderId)
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .status(DisputeStatus.RESOLVED_REFUND)
                    .build();
            dispute.setId(disputeId);

            SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
            subOrder.setId(subOrderId);
            subOrder.setVendorId(vendorId);

            when(disputeRepository.findByIdForUpdate(disputeId)).thenReturn(Optional.of(dispute));
            when(subOrderRepository.findById(subOrderId)).thenReturn(Optional.of(subOrder));

            SubmitVendorResponseRequest request = new SubmitVendorResponseRequest(
                    "Attempting response after resolution", List.of());

            assertThatThrownBy(() -> useCase.execute(disputeId, request, vendorUserId))
                    .isInstanceOf(DisputeAlreadyResolvedException.class);
        }
    }

    @Nested
    @DisplayName("GetAdminDisputeDetailUseCase Tests")
    class GetAdminDisputeDetailUseCaseTests {

        @Test
        @DisplayName("Returns full dispute dossier for Admin")
        void execute_Success() {
            GetAdminDisputeDetailUseCase useCase = new GetAdminDisputeDetailUseCase(disputeRepository);

            DisputeJpaEntity dispute = DisputeJpaEntity.builder()
                    .orderId(orderId)
                    .subOrderId(subOrderId)
                    .customerId(customerId)
                    .status(DisputeStatus.UNDER_REVIEW)
                    .vendorResponse("Vendor feedback")
                    .vendorRespondedAt(OffsetDateTime.now())
                    .build();
            dispute.setId(disputeId);

            when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));

            DisputeResponse response = useCase.execute(disputeId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(disputeId);
            assertThat(response.vendorResponse()).isEqualTo("Vendor feedback");
        }

        @Test
        @DisplayName("Throws DisputeNotFoundException when dispute not found")
        void execute_NotFound() {
            GetAdminDisputeDetailUseCase useCase = new GetAdminDisputeDetailUseCase(disputeRepository);
            when(disputeRepository.findById(disputeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(disputeId))
                    .isInstanceOf(DisputeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("UploadDisputeEvidenceUseCase Tests")
    class UploadDisputeEvidenceUseCaseTests {

        @Test
        @DisplayName("Uploads valid JPEG file and returns secure URL")
        void execute_Success() {
            UploadDisputeEvidenceUseCase useCase = new UploadDisputeEvidenceUseCase(fileStoragePort);

            byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", jpegBytes);

            when(fileStoragePort.uploadFile(any(byte[].class), eq("photo.jpg"), eq("disputes/evidence")))
                    .thenReturn("https://res.cloudinary.com/test/photo.jpg");

            EvidenceUploadResponse response = useCase.execute(file);

            assertThat(response).isNotNull();
            assertThat(response.fileUrl()).isEqualTo("https://res.cloudinary.com/test/photo.jpg");
        }
    }
}
