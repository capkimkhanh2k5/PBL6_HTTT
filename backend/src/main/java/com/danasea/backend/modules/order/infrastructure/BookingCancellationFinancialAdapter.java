package com.danasea.backend.modules.order.infrastructure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.booking.domain.models.CancellationFinancialResult;
import com.danasea.backend.modules.booking.domain.ports.BookingCancellationFinancialPort;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.RefundEvaluationResult;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaRefundRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;

@Component
public class BookingCancellationFinancialAdapter implements BookingCancellationFinancialPort {

    private final JpaMasterOrderRepository masterOrderRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final JpaRefundRepository refundRepository;
    private final JpaServiceSlotRepository serviceSlotRepository;
    private final RefundPolicyEngine refundPolicyEngine;

    public BookingCancellationFinancialAdapter(
            JpaMasterOrderRepository masterOrderRepository,
            JpaSubOrderRepository subOrderRepository,
            JpaRefundRepository refundRepository,
            JpaServiceSlotRepository serviceSlotRepository,
            RefundPolicyEngine refundPolicyEngine) {
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.refundRepository = refundRepository;
        this.serviceSlotRepository = serviceSlotRepository;
        this.refundPolicyEngine = refundPolicyEngine;
    }

    @Override
    public CancellationFinancialResult requestRefund(
            UUID bookingId,
            UUID requestedBy,
            String idempotencyKey,
            OffsetDateTime requestedAt) {
        var order = masterOrderRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new InvalidOrderStateException("Paid booking has no associated order."));
        if (!MasterOrderStatus.PAID.equals(order.getStatus())) {
            throw new InvalidOrderStateException("Only a paid order can enter the cancellation refund flow.");
        }

        List<SubOrderJpaEntity> subOrders = subOrderRepository.findByMasterOrderId(order.getId());
        BigDecimal originalTotal = BigDecimal.ZERO;
        BigDecimal refundTotal = BigDecimal.ZERO;
        for (SubOrderJpaEntity subOrder : subOrders) {
            originalTotal = originalTotal.add(subOrder.getSubtotalAmount());
            var existing = refundRepository.findBySubOrderIdAndIdempotencyKey(subOrder.getId(), idempotencyKey);
            if (existing.isPresent()) {
                refundTotal = refundTotal.add(existing.get().getAmount());
                subOrder.setStatus(SubOrderStatus.CANCELLED);
                continue;
            }

            LocalDateTime departure = serviceSlotRepository.findById(subOrder.getSlotId())
                    .filter(slot -> slot.getDate() != null && slot.getStartTime() != null)
                    .map(slot -> LocalDateTime.of(slot.getDate(), slot.getStartTime()))
                    .orElse(null);
            RefundEvaluationResult evaluation = refundPolicyEngine.evaluate(
                    RefundReason.CUSTOMER_CANCEL,
                    departure,
                    requestedAt.toLocalDateTime(),
                    subOrder.getSubtotalAmount());
            refundTotal = refundTotal.add(evaluation.refundAmount());

            if (evaluation.refundAmount().compareTo(BigDecimal.ZERO) > 0) {
                RefundJpaEntity refund = new RefundJpaEntity();
                refund.setSubOrderId(subOrder.getId());
                refund.setAmount(evaluation.refundAmount());
                refund.setRefundPercentage(evaluation.refundPercentage());
                refund.setReason(RefundReason.CUSTOMER_CANCEL);
                refund.setStatus(RefundStatus.PENDING);
                refund.setRequestedBy(requestedBy);
                refund.setIdempotencyKey(idempotencyKey);
                refundRepository.save(refund);
            }
            subOrder.setStatus(SubOrderStatus.CANCELLED);
        }
        subOrderRepository.saveAll(subOrders);
        order.setStatus(MasterOrderStatus.CANCELLED);
        masterOrderRepository.save(order);

        int percentage = originalTotal.compareTo(BigDecimal.ZERO) == 0
                ? 0
                : refundTotal.multiply(BigDecimal.valueOf(100))
                        .divide(originalTotal, 0, RoundingMode.HALF_UP).intValue();
        return new CancellationFinancialResult(
                refundTotal.compareTo(BigDecimal.ZERO) > 0,
                percentage,
                refundTotal);
    }

    @Override
    public void cancelUnpaidOrder(UUID bookingId) {
        masterOrderRepository.findByBookingId(bookingId).ifPresent(order -> {
            if (!MasterOrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
                throw new InvalidOrderStateException("Only a pending-payment order can be cancelled without a refund.");
            }
            List<SubOrderJpaEntity> subOrders = subOrderRepository.findByMasterOrderId(order.getId());
            subOrders.forEach(subOrder -> subOrder.setStatus(SubOrderStatus.CANCELLED));
            subOrderRepository.saveAll(subOrders);
            order.setStatus(MasterOrderStatus.CANCELLED);
            masterOrderRepository.save(order);
        });
    }
}
