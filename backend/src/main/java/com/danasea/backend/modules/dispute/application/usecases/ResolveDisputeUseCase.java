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
        // 1. Tìm Dispute
        DisputeJpaEntity dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException("Dispute not found with id: " + disputeId));

        // 2. Idempotency Guard
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

            // Tạo và lưu RefundJpaEntity
            RefundJpaEntity refund = new RefundJpaEntity();
            refund.setSubOrderId(subOrder.getId());
            refund.setAmount(refundAmount);
            refund.setRefundPercentage(appliedRefundPercentage);
            refund.setReason(RefundReason.ADMIN_OVERRIDE);
            refund.setStatus(RefundStatus.PROCESSED);
            refund.setProcessedAt(now);
            refundRepository.save(refund);

            // Cập nhật trạng thái SubOrder
            if (appliedRefundPercentage.compareTo(BigDecimal.valueOf(100)) == 0) {
                subOrder.setStatus(SubOrderStatus.REFUNDED);
            } else {
                subOrder.setStatus(SubOrderStatus.PARTIALLY_REFUNDED);
            }
            subOrderRepository.save(subOrder);

            log.info("Dispute {} resolved with refund: {} ({}%), SubOrder {} updated to {}",
                    disputeId, refundAmount, appliedRefundPercentage, subOrder.getId(), subOrder.getStatus());
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
