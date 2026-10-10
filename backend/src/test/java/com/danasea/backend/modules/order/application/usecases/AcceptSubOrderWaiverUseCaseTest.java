package com.danasea.backend.modules.order.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.modules.order.application.dtos.AcceptSubOrderWaiverCommand;
import com.danasea.backend.modules.order.application.dtos.SubOrderWaiverAcceptanceResult;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.UnauthorizedOrderAccessException;
import com.danasea.backend.modules.order.domain.exceptions.WaiverVersionMismatchException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("AcceptSubOrderWaiverUseCase Tests")
class AcceptSubOrderWaiverUseCaseTest {

    @Mock private MasterOrderRepositoryPort masterOrderRepository;
    @Mock private SubOrderRepositoryPort subOrderRepository;
    @Mock private AuditLogInternalApi auditLogInternalApi;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AcceptSubOrderWaiverUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID otherCustomerId = UUID.randomUUID();
    private final UUID masterOrderId = UUID.randomUUID();
    private final UUID subOrderId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase =
                new AcceptSubOrderWaiverUseCase(
                        masterOrderRepository,
                        subOrderRepository,
                        objectMapper,
                        auditLogInternalApi);
    }

    private MasterOrder createMasterOrder(UUID ownerId, MasterOrderStatus status) {
        MasterOrder order = new MasterOrder();
        order.setId(masterOrderId);
        order.setCustomerId(ownerId);
        order.setStatus(status);
        return order;
    }

    private SubOrder createSubOrder(
            boolean required, int version, String vi, String en, boolean accepted) {
        SubOrder sub = new SubOrder();
        sub.setId(subOrderId);
        sub.setMasterOrderId(masterOrderId);
        sub.setServiceId(serviceId);
        sub.setStatus(SubOrderStatus.PENDING);
        sub.setWaiverRequired(required);
        sub.setWaiverVersion(version);
        sub.setWaiverContent(vi);
        sub.setWaiverContentEn(en);
        sub.setWaiverAccepted(accepted);
        return sub;
    }

    @Nested
    @DisplayName("Chấp thuận thành công và Idempotency")
    class SuccessAndIdempotencyTests {

        @Test
        @DisplayName(
                "Xác nhận thành công tiếng Việt: lưu JWT user, server clock, content VI snapshot và"
                        + " ghi Audit")
        void acceptWaiverSuccessVietnamese() {
            MasterOrder order = createMasterOrder(customerId, MasterOrderStatus.PENDING_PAYMENT);
            SubOrder subOrder =
                    createSubOrder(true, 1, "Cam kết lặn biển VI", "Diving waiver EN", false);

            when(subOrderRepository.findMasterOrderIdById(subOrderId))
                    .thenReturn(Optional.of(subOrder.getMasterOrderId()));
            when(masterOrderRepository.findByIdForUpdate(masterOrderId))
                    .thenReturn(Optional.of(order));
            when(subOrderRepository.findByIdForUpdate(subOrderId))
                    .thenReturn(Optional.of(subOrder));
            when(subOrderRepository.save(any(SubOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AcceptSubOrderWaiverCommand cmd = new AcceptSubOrderWaiverCommand(true, 1, "vi");
            OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);

            SubOrderWaiverAcceptanceResult result = useCase.execute(customerId, subOrderId, cmd);

            assertThat(result.accepted()).isTrue();
            assertThat(result.waiverVersion()).isEqualTo(1);
            assertThat(result.language()).isEqualTo("VI");
            assertThat(result.acceptedAt()).isAfterOrEqualTo(before);

            ArgumentCaptor<SubOrder> captor = ArgumentCaptor.forClass(SubOrder.class);
            verify(subOrderRepository).save(captor.capture());
            SubOrder saved = captor.getValue();

            assertThat(saved.getWaiverAccepted()).isTrue();
            assertThat(saved.getWaiverAcceptedBy()).isEqualTo(customerId);
            assertThat(saved.getWaiverAcceptedLanguage()).isEqualTo("VI");
            assertThat(saved.getWaiverAcceptedContent()).isEqualTo("Cam kết lặn biển VI");

            verify(auditLogInternalApi)
                    .recordTransactionalAuditLog(
                            eq(customerId),
                            eq("SUB_ORDER_WAIVER_ACCEPTED"),
                            eq("SUB_ORDER"),
                            eq(subOrderId),
                            any());
        }

        @Test
        @DisplayName("Xác nhận thành công tiếng Anh: lưu content EN snapshot")
        void acceptWaiverSuccessEnglish() {
            MasterOrder order = createMasterOrder(customerId, MasterOrderStatus.PENDING_PAYMENT);
            SubOrder subOrder = createSubOrder(true, 2, "Cam kết VI", "Diving waiver EN", false);

            when(subOrderRepository.findMasterOrderIdById(subOrderId))
                    .thenReturn(Optional.of(subOrder.getMasterOrderId()));
            when(masterOrderRepository.findByIdForUpdate(masterOrderId))
                    .thenReturn(Optional.of(order));
            when(subOrderRepository.findByIdForUpdate(subOrderId))
                    .thenReturn(Optional.of(subOrder));
            when(subOrderRepository.save(any(SubOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AcceptSubOrderWaiverCommand cmd = new AcceptSubOrderWaiverCommand(true, 2, "EN");
            SubOrderWaiverAcceptanceResult result = useCase.execute(customerId, subOrderId, cmd);

            assertThat(result.language()).isEqualTo("EN");
            ArgumentCaptor<SubOrder> captor = ArgumentCaptor.forClass(SubOrder.class);
            verify(subOrderRepository).save(captor.capture());
            assertThat(captor.getValue().getWaiverAcceptedContent()).isEqualTo("Diving waiver EN");
        }

        @Test
        void englishRequestWithVietnameseFallbackStoresActualLanguage() {
            MasterOrder order = createMasterOrder(customerId, MasterOrderStatus.PENDING_PAYMENT);
            SubOrder subOrder = createSubOrder(true, 1, "Vietnamese snapshot", " ", false);
            when(subOrderRepository.findMasterOrderIdById(subOrderId))
                    .thenReturn(Optional.of(subOrder.getMasterOrderId()));
            when(masterOrderRepository.findByIdForUpdate(masterOrderId))
                    .thenReturn(Optional.of(order));
            when(subOrderRepository.findByIdForUpdate(subOrderId))
                    .thenReturn(Optional.of(subOrder));
            when(subOrderRepository.save(any(SubOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            var result =
                    useCase.execute(
                            customerId, subOrderId, new AcceptSubOrderWaiverCommand(true, 1, "EN"));
            assertThat(result.language()).isEqualTo("VI");
            assertThat(subOrder.getWaiverAcceptedLanguage()).isEqualTo("VI");
            assertThat(subOrder.getWaiverAcceptedContent()).isEqualTo("Vietnamese snapshot");
        }

        @Test
        @DisplayName(
                "Idempotent Replay: Gửi lại cùng version đã accept trước đó -> trả về kết quả cũ,"
                        + " KHÔNG đổi acceptedAt, KHÔNG ghi audit lặp")
        void idempotentReplayReturnsExistingWithoutModifyingOrAuditing() {
            MasterOrder order = createMasterOrder(customerId, MasterOrderStatus.PENDING_PAYMENT);
            SubOrder subOrder = createSubOrder(true, 1, "Cam kết VI", "Waiver EN", true);
            OffsetDateTime originalAcceptedAt = OffsetDateTime.now().minusHours(1);
            subOrder.setWaiverAcceptedAt(originalAcceptedAt);
            subOrder.setWaiverAcceptedBy(customerId);
            subOrder.setWaiverAcceptedLanguage("VI");
            subOrder.setWaiverAcceptedContent("Cam kết VI");

            when(subOrderRepository.findMasterOrderIdById(subOrderId))
                    .thenReturn(Optional.of(subOrder.getMasterOrderId()));
            when(masterOrderRepository.findByIdForUpdate(masterOrderId))
                    .thenReturn(Optional.of(order));
            when(subOrderRepository.findByIdForUpdate(subOrderId))
                    .thenReturn(Optional.of(subOrder));

            AcceptSubOrderWaiverCommand cmd = new AcceptSubOrderWaiverCommand(true, 1, "VI");
            SubOrderWaiverAcceptanceResult result = useCase.execute(customerId, subOrderId, cmd);

            assertThat(result.accepted()).isTrue();
            assertThat(result.acceptedAt()).isEqualTo(originalAcceptedAt);

            verify(subOrderRepository, never()).save(any());
            verify(auditLogInternalApi, never())
                    .recordTransactionalAuditLog(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Validation và Kiểm tra quyền")
    class ValidationAndSecurityTests {

        @Test
        @DisplayName("Khác chủ sở hữu đơn ném UnauthorizedOrderAccessException (403)")
        void wrongOwnerThrowsUnauthorized() {
            MasterOrder order = createMasterOrder(customerId, MasterOrderStatus.PENDING_PAYMENT);
            SubOrder subOrder = createSubOrder(true, 1, "VI", "EN", false);

            when(subOrderRepository.findMasterOrderIdById(subOrderId))
                    .thenReturn(Optional.of(subOrder.getMasterOrderId()));
            when(masterOrderRepository.findByIdForUpdate(masterOrderId))
                    .thenReturn(Optional.of(order));
            when(subOrderRepository.findByIdForUpdate(subOrderId))
                    .thenReturn(Optional.of(subOrder));

            AcceptSubOrderWaiverCommand cmd = new AcceptSubOrderWaiverCommand(true, 1, "VI");

            assertThatThrownBy(() -> useCase.execute(otherCustomerId, subOrderId, cmd))
                    .isInstanceOf(UnauthorizedOrderAccessException.class);
        }

        @Test
        @DisplayName("Đơn không ở trạng thái PENDING_PAYMENT ném InvalidOrderStateException (409)")
        void notPendingPaymentOrderThrows() {
            MasterOrder order = createMasterOrder(customerId, MasterOrderStatus.PAID);
            SubOrder subOrder = createSubOrder(true, 1, "VI", "EN", false);

            when(subOrderRepository.findMasterOrderIdById(subOrderId))
                    .thenReturn(Optional.of(subOrder.getMasterOrderId()));
            when(masterOrderRepository.findByIdForUpdate(masterOrderId))
                    .thenReturn(Optional.of(order));
            when(subOrderRepository.findByIdForUpdate(subOrderId))
                    .thenReturn(Optional.of(subOrder));

            AcceptSubOrderWaiverCommand cmd = new AcceptSubOrderWaiverCommand(true, 1, "VI");

            assertThatThrownBy(() -> useCase.execute(customerId, subOrderId, cmd))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("PENDING_PAYMENT");
        }

        @Test
        @DisplayName("Sub-order không yêu cầu cam kết ném InvalidOrderStateException (409)")
        void waiverNotRequiredThrows() {
            MasterOrder order = createMasterOrder(customerId, MasterOrderStatus.PENDING_PAYMENT);
            SubOrder subOrder = createSubOrder(false, 1, null, null, false);

            when(subOrderRepository.findMasterOrderIdById(subOrderId))
                    .thenReturn(Optional.of(subOrder.getMasterOrderId()));
            when(masterOrderRepository.findByIdForUpdate(masterOrderId))
                    .thenReturn(Optional.of(order));
            when(subOrderRepository.findByIdForUpdate(subOrderId))
                    .thenReturn(Optional.of(subOrder));

            AcceptSubOrderWaiverCommand cmd = new AcceptSubOrderWaiverCommand(true, 1, "VI");

            assertThatThrownBy(() -> useCase.execute(customerId, subOrderId, cmd))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("not required");
        }

        @Test
        @DisplayName("Client gửi sai waiver version ném WaiverVersionMismatchException (409)")
        void versionMismatchThrows() {
            MasterOrder order = createMasterOrder(customerId, MasterOrderStatus.PENDING_PAYMENT);
            SubOrder subOrder = createSubOrder(true, 2, "VI", "EN", false);

            when(subOrderRepository.findMasterOrderIdById(subOrderId))
                    .thenReturn(Optional.of(subOrder.getMasterOrderId()));
            when(masterOrderRepository.findByIdForUpdate(masterOrderId))
                    .thenReturn(Optional.of(order));
            when(subOrderRepository.findByIdForUpdate(subOrderId))
                    .thenReturn(Optional.of(subOrder));

            // Client gửi version 1 trong khi snapshot là version 2
            AcceptSubOrderWaiverCommand cmd = new AcceptSubOrderWaiverCommand(true, 1, "VI");

            assertThatThrownBy(() -> useCase.execute(customerId, subOrderId, cmd))
                    .isInstanceOf(WaiverVersionMismatchException.class)
                    .hasMessageContaining("mismatch");
        }

        @Test
        @DisplayName("Ngôn ngữ không được hỗ trợ ném IllegalArgumentException")
        void unsupportedLanguageThrows() {
            MasterOrder order = createMasterOrder(customerId, MasterOrderStatus.PENDING_PAYMENT);
            SubOrder subOrder = createSubOrder(true, 1, "VI", "EN", false);

            when(subOrderRepository.findMasterOrderIdById(subOrderId))
                    .thenReturn(Optional.of(subOrder.getMasterOrderId()));
            when(masterOrderRepository.findByIdForUpdate(masterOrderId))
                    .thenReturn(Optional.of(order));
            when(subOrderRepository.findByIdForUpdate(subOrderId))
                    .thenReturn(Optional.of(subOrder));

            AcceptSubOrderWaiverCommand cmd = new AcceptSubOrderWaiverCommand(true, 1, "FR");

            assertThatThrownBy(() -> useCase.execute(customerId, subOrderId, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supported languages");
        }

        @Test
        @DisplayName("Client không gửi accepted=true ném IllegalArgumentException")
        void acceptedFalseThrows() {
            AcceptSubOrderWaiverCommand cmd = new AcceptSubOrderWaiverCommand(false, 1, "VI");

            assertThatThrownBy(() -> useCase.execute(customerId, subOrderId, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("accepted");
        }

        @Test
        @DisplayName("User chưa đăng nhập (customerId == null) ném AccessDeniedException")
        void nullCustomerIdThrows() {
            AcceptSubOrderWaiverCommand cmd = new AcceptSubOrderWaiverCommand(true, 1, "VI");

            assertThatThrownBy(() -> useCase.execute(null, subOrderId, cmd))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}
