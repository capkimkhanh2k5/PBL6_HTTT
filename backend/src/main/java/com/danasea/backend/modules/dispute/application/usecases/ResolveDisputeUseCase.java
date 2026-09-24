package com.danasea.backend.modules.dispute.application.usecases;

import com.danasea.backend.modules.dispute.domain.exceptions.DisputeAlreadyResolvedException;
import com.danasea.backend.modules.dispute.domain.exceptions.DisputeNotFoundException;
import com.danasea.backend.modules.dispute.domain.exceptions.InvalidDisputeResolutionException;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.dispute.infrastructure.persistence.entities.DisputeJpaEntity;
import com.danasea.backend.modules.dispute.infrastructure.persistence.repositories.JpaDisputeRepository;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import com.danasea.backend.modules.dispute.presentation.dtos.ResolveDisputeRequest;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResolveDisputeUseCase {

    private final JpaDisputeRepository disputeRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final JpaRefundRepository refundRepository;

    @Transactional
    public DisputeResponse execute(UUID disputeId, ResolveDisputeRequest request) {
        UUID adminUserId = SecurityUtils.getCurrentUserId().orElse(null);
        return execute(disputeId, request, adminUserId);
    }

    @Transactional
    public DisputeResponse execute(UUID disputeId, ResolveDisputeRequest request, UUID adminUserId) {
        if (request == null || request.resolution() == null) {
            throw new InvalidDisputeResolutionException("A resolution action is required.");
        }
        if (adminUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Administrator authentication is required.");
        }

        DisputeJpaEntity dispute = disputeRepository.findByIdForUpdate(disputeId)
                .or(() -> disputeRepository.findById(disputeId))
                .orElseThrow(() -> new DisputeNotFoundException("Dispute not found with id: " + disputeId));

        if (dispute.getStatus() != null && dispute.getStatus().isResolved()) {
            throw new DisputeAlreadyResolvedException(
                    "Dispute with id " + disputeId + " is already resolved with status " + dispute.getStatus());
        }

        // 3. Validate Resolution Action
        DisputeStatus resolution = request.resolution();
        if (resolution != DisputeStatus.RESOLVED_REFUND
                && resolution != DisputeStatus.RESOLVED_PARTIAL
                && resolution != DisputeStatus.RESOLVED_REJECTED) {
            throw new InvalidDisputeResolutionException(
                    "Resolution status must be one of: RESOLVED_REFUND, RESOLVED_PARTIAL, RESOLVED_REJECTED");
        }

        OffsetDateTime now = OffsetDateTime.now();
        BigDecimal appliedRefundPercentage = null;

        // 4. Phân xử tài chính
        if (resolution == DisputeStatus.RESOLVED_REFUND || resolution == DisputeStatus.RESOLVED_PARTIAL) {
            SubOrderJpaEntity subOrder = subOrderRepository.findById(dispute.getSubOrderId())
                    .orElseThrow(() -> new OrderNotFoundException(
                            "Sub-order not found with id: " + dispute.getSubOrderId()));

            appliedRefundPercentage = request.refundPercentage();
            if (resolution == DisputeStatus.RESOLVED_REFUND && appliedRefundPercentage == null) {
                appliedRefundPercentage = BigDecimal.valueOf(100.0);
            }

            if (appliedRefundPercentage == null
                    || appliedRefundPercentage.compareTo(BigDecimal.ZERO) <= 0
                    || appliedRefundPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException(
                        "Refund percentage must be greater than 0 and less than or equal to 100");
            }

            // Tính toán Refund Amount: subtotalAmount * (refundPercentage / 100)
            BigDecimal subtotal = subOrder.getSubtotalAmount() != null ? subOrder.getSubtotalAmount() : BigDecimal.ZERO;
            BigDecimal refundAmount = subtotal.multiply(appliedRefundPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            String idempotencyKey = "dispute-" + disputeId;
            if (refundRepository.findBySubOrderIdAndIdempotencyKey(subOrder.getId(), idempotencyKey).isEmpty()) {
                RefundJpaEntity refund = new RefundJpaEntity();
                refund.setSubOrderId(subOrder.getId());
                refund.setAmount(refundAmount);
                refund.setRefundPercentage(appliedRefundPercentage);
                refund.setReason(RefundReason.ADMIN_OVERRIDE);
                refund.setStatus(RefundStatus.PENDING);
                refund.setRequestedBy(adminUserId);
                refund.setIdempotencyKey(idempotencyKey);
                refundRepository.save(refund);
            }

            log.info("Dispute {} approved refund request: {} ({}%) for sub-order {}. Provider processing is pending.",
                    disputeId, refundAmount, appliedRefundPercentage, subOrder.getId());
        } else {
            // RESOLVED_REJECTED: Không tạo refund, không đổi SubOrder
            log.info("Dispute {} rejected by admin {}. No refund generated.", disputeId, adminUserId);
        }

        // 5. Cập nhật bản ghi Dispute
        dispute.setStatus(resolution);
        dispute.setRefundPercentage(appliedRefundPercentage);
        dispute.setAdminNote(request.adminNote());
        dispute.setResolvedBy(adminUserId);
        dispute.setResolvedAt(now);
        DisputeJpaEntity savedDispute = disputeRepository.save(dispute);

        return toResponse(savedDispute);
    }

    private DisputeResponse toResponse(DisputeJpaEntity savedDispute) {
        return new DisputeResponse(
                savedDispute.getId(),
                savedDispute.getOrderId(),
                savedDispute.getSubOrderId(),
                savedDispute.getCustomerId(),
                savedDispute.getReason(),
                savedDispute.getDescription(),
                savedDispute.getEvidenceUrls(),
                savedDispute.getStatus(),
                savedDispute.getRefundPercentage(),
                savedDispute.getAdminNote(),
                savedDispute.getResolvedBy(),
                savedDispute.getResolvedAt(),
                savedDispute.getCreatedAt(),
                savedDispute.getUpdatedAt()
        );
    }
}
