package com.danasea.backend.modules.order.application.usecases;

import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.modules.order.application.dtos.AcceptSubOrderWaiverCommand;
import com.danasea.backend.modules.order.application.dtos.SubOrderWaiverAcceptanceResult;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.exceptions.UnauthorizedOrderAccessException;
import com.danasea.backend.modules.order.domain.exceptions.WaiverVersionMismatchException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AcceptSubOrderWaiverUseCase {

    private final MasterOrderRepositoryPort masterOrderRepository;
    private final SubOrderRepositoryPort subOrderRepository;
    private final ObjectMapper objectMapper;
    private final AuditLogInternalApi auditLogInternalApi;

    @Autowired
    public AcceptSubOrderWaiverUseCase(
            MasterOrderRepositoryPort masterOrderRepository,
            SubOrderRepositoryPort subOrderRepository,
            ObjectMapper objectMapper,
            @Autowired(required = false) AuditLogInternalApi auditLogInternalApi) {
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.objectMapper = objectMapper;
        this.auditLogInternalApi = auditLogInternalApi;
    }

    public AcceptSubOrderWaiverUseCase(
            MasterOrderRepositoryPort masterOrderRepository,
            SubOrderRepositoryPort subOrderRepository,
            ObjectMapper objectMapper) {
        this(masterOrderRepository, subOrderRepository, objectMapper, null);
    }

    @Transactional
    public SubOrderWaiverAcceptanceResult execute(
            UUID customerId, UUID subOrderId, AcceptSubOrderWaiverCommand command) {
        if (customerId == null) {
            throw new AccessDeniedException("User is not authenticated");
        }
        if (subOrderId == null) {
            throw new IllegalArgumentException("Sub-order ID is required.");
        }
        if (command == null || !Boolean.TRUE.equals(command.accepted())) {
            throw new IllegalArgumentException("Waiver must be explicitly accepted.");
        }
        if (command.version() == null || command.version() < 1) {
            throw new IllegalArgumentException("A valid waiver version is required.");
        }
        if (command.language() == null || command.language().isBlank()) {
            throw new IllegalArgumentException("Language is required.");
        }

        // 1. Tìm sơ bộ SubOrder để biết masterOrderId
        UUID masterOrderId =
                subOrderRepository
                        .findMasterOrderIdById(subOrderId)
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                "Sub-order not found with id: " + subOrderId));

        // 2. Khóa MasterOrder trước (khóa theo thứ tự nhất quán: MasterOrder -> SubOrder)
        MasterOrder masterOrder =
                masterOrderRepository
                        .findByIdForUpdate(masterOrderId)
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                "Master order not found with id: "
                                                        + masterOrderId));

        // 3. Khóa SubOrder
        SubOrder subOrder =
                subOrderRepository
                        .findByIdForUpdate(subOrderId)
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                "Sub-order not found with id: " + subOrderId));

        // 4. Kiểm tra quyền sở hữu đơn
        if (!masterOrder.getCustomerId().equals(customerId)) {
            throw new UnauthorizedOrderAccessException(masterOrder.getId(), customerId);
        }

        // 5. Kiểm tra trạng thái đơn và sub-order
        if (masterOrder.getStatus() != MasterOrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException(
                    "Waiver can only be accepted for orders in PENDING_PAYMENT status; current: "
                            + masterOrder.getStatus());
        }
        if (subOrder.getStatus() != SubOrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Waiver can only be accepted for sub-orders in PENDING status; current: "
                            + subOrder.getStatus());
        }
        if (!Boolean.TRUE.equals(subOrder.getWaiverRequired())) {
            throw new InvalidOrderStateException(
                    "Safety waiver is not required for this sub-order.");
        }

        // 6. Kiểm tra phiên bản cam kết
        if (!Objects.equals(command.version(), subOrder.getWaiverVersion())) {
            throw new WaiverVersionMismatchException(
                    "Waiver version mismatch. Expected version: "
                            + subOrder.getWaiverVersion()
                            + ", but got: "
                            + command.version());
        }

        // 7. Kiểm tra ngôn ngữ và đối chiếu nội dung trong snapshot
        String lang = command.language().trim().toUpperCase(Locale.ROOT);
        if (!"VI".equals(lang) && !"EN".equals(lang)) {
            throw new IllegalArgumentException(
                    "Unsupported language: "
                            + command.language()
                            + ". Supported languages are VI and EN.");
        }

        String agreedContent;
        String acceptedLanguage;
        if ("VI".equals(lang)) {
            agreedContent =
                    (subOrder.getWaiverContent() != null && !subOrder.getWaiverContent().isBlank())
                            ? subOrder.getWaiverContent()
                            : subOrder.getWaiverContentEn();
            acceptedLanguage =
                    subOrder.getWaiverContent() != null && !subOrder.getWaiverContent().isBlank()
                            ? "VI"
                            : "EN";
        } else {
            agreedContent =
                    (subOrder.getWaiverContentEn() != null
                                    && !subOrder.getWaiverContentEn().isBlank())
                            ? subOrder.getWaiverContentEn()
                            : subOrder.getWaiverContent();
            acceptedLanguage =
                    subOrder.getWaiverContentEn() != null
                                    && !subOrder.getWaiverContentEn().isBlank()
                            ? "EN"
                            : "VI";
        }

        if (agreedContent == null || agreedContent.isBlank()) {
            throw new InvalidOrderStateException("No waiver content available in snapshot.");
        }

        // 8. Idempotent Replay: nếu đã chấp nhận với cùng phiên bản trước đó, trả kết quả đã lưu mà
        // không cập nhật lại thời gian hoặc ghi audit trùng
        if (Boolean.TRUE.equals(subOrder.getWaiverAccepted())) {
            return new SubOrderWaiverAcceptanceResult(
                    subOrder.getId(),
                    subOrder.getMasterOrderId(),
                    true,
                    subOrder.getWaiverVersion(),
                    subOrder.getWaiverAcceptedLanguage() != null
                            ? subOrder.getWaiverAcceptedLanguage()
                            : lang,
                    subOrder.getWaiverAcceptedAt());
        }

        // 9. Ghi nhận chấp thuận cam kết mới với clock của server và JWT user
        OffsetDateTime now = OffsetDateTime.now();
        subOrder.setWaiverAccepted(true);
        subOrder.setWaiverAcceptedAt(now);
        subOrder.setWaiverAcceptedBy(customerId);
        subOrder.setWaiverAcceptedLanguage(acceptedLanguage);
        subOrder.setWaiverAcceptedContent(agreedContent);
        subOrderRepository.save(subOrder);

        // 10. Ghi Audit Log
        if (auditLogInternalApi != null) {
            try {
                Map<String, Object> metadata =
                        Map.of(
                                "subOrderId", subOrder.getId().toString(),
                                "masterOrderId", subOrder.getMasterOrderId().toString(),
                                "serviceId", subOrder.getServiceId().toString(),
                                "waiverVersion", subOrder.getWaiverVersion(),
                                "language", acceptedLanguage);
                auditLogInternalApi.recordTransactionalAuditLog(
                        customerId,
                        "SUB_ORDER_WAIVER_ACCEPTED",
                        "SUB_ORDER",
                        subOrder.getId(),
                        objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(
                        "Failed to serialize waiver acceptance audit log metadata", ex);
            }
        }

        return new SubOrderWaiverAcceptanceResult(
                subOrder.getId(),
                subOrder.getMasterOrderId(),
                true,
                subOrder.getWaiverVersion(),
                acceptedLanguage,
                now);
    }
}
