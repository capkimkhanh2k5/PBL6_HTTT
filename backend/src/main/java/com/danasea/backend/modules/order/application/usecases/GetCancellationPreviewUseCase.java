package com.danasea.backend.modules.order.application.usecases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.application.dtos.CancellationPreviewResult;
import com.danasea.backend.modules.order.application.dtos.GetCancellationPreviewQuery;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.RefundEvaluationResult;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.ServiceSlotDepartureLookupPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;

@Service
public class GetCancellationPreviewUseCase {

    private final MasterOrderRepositoryPort masterOrderRepository;
    private final SubOrderRepositoryPort subOrderRepository;
    private final ServiceSlotDepartureLookupPort serviceSlotDepartureLookupPort;
    private final RefundPolicyEngine refundPolicyEngine;

    public GetCancellationPreviewUseCase(
            MasterOrderRepositoryPort masterOrderRepository,
            SubOrderRepositoryPort subOrderRepository,
            ServiceSlotDepartureLookupPort serviceSlotDepartureLookupPort,
            RefundPolicyEngine refundPolicyEngine) {
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.serviceSlotDepartureLookupPort = serviceSlotDepartureLookupPort;
        this.refundPolicyEngine = refundPolicyEngine;
    }

    @Transactional(readOnly = true)
    public CancellationPreviewResult execute(GetCancellationPreviewQuery query) {
        if (query == null || query.orderOrSubOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required.");
        }

        MasterOrder masterOrder;
        List<SubOrder> subOrders;

        Optional<MasterOrder> masterOpt = masterOrderRepository.findById(query.orderOrSubOrderId());
        if (masterOpt.isPresent()) {
            masterOrder = masterOpt.get();
            subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        } else {
            SubOrder subOrder = subOrderRepository.findById(query.orderOrSubOrderId())
                    .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + query.orderOrSubOrderId()));
            masterOrder = masterOrderRepository.findById(subOrder.getMasterOrderId())
                    .orElseThrow(() -> new OrderNotFoundException("Master order not found for sub-order: " + query.orderOrSubOrderId()));
            subOrders = List.of(subOrder);
        }

        // Chặn IDOR: Người dùng chỉ được xem trước hủy đơn của chính mình trừ khi là Quản trị viên (ADMIN)
        if (!query.isAdmin() && (masterOrder.getCustomerId() == null || !masterOrder.getCustomerId().equals(query.currentUserId()))) {
            throw new AccessDeniedException("User does not have permission to view cancellation preview for this order");
        }

        LocalDateTime now = LocalDateTime.now();
        List<CancellationPreviewResult.SubOrderCancellationPreviewResult> items = new ArrayList<>();
        BigDecimal totalOriginalAmount = BigDecimal.ZERO;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (SubOrder subOrder : subOrders) {
            LocalDateTime departureTime = subOrder.getSlotId() != null
                    ? serviceSlotDepartureLookupPort.findDepartureTime(subOrder.getSlotId()).orElse(null)
                    : null;

            BigDecimal originalAmount = subOrder.getSubtotalAmount() != null
                    ? subOrder.getSubtotalAmount()
                    : BigDecimal.ZERO;

            RefundEvaluationResult eval = refundPolicyEngine.evaluate(
                    RefundReason.CUSTOMER_REQUEST,
                    departureTime,
                    now,
                    originalAmount
            );

            String policyApplied = refundPolicyEngine.getPolicyDescription(RefundReason.CUSTOMER_REQUEST, eval.refundPercentage());

            items.add(new CancellationPreviewResult.SubOrderCancellationPreviewResult(
                    subOrder.getId(),
                    subOrder.getServiceId(),
                    subOrder.getSlotId(),
                    departureTime,
                    originalAmount,
                    eval.refundPercentage(),
                    eval.refundAmount(),
                    policyApplied
            ));

            totalOriginalAmount = totalOriginalAmount.add(originalAmount);
            totalRefundAmount = totalRefundAmount.add(eval.refundAmount());
        }

        BigDecimal overallPercentage;
        if (totalOriginalAmount.compareTo(BigDecimal.ZERO) > 0) {
            overallPercentage = totalRefundAmount.multiply(BigDecimal.valueOf(100.0))
                    .divide(totalOriginalAmount, 1, RoundingMode.HALF_UP);
        } else {
            overallPercentage = BigDecimal.valueOf(0.0);
        }

        String masterPolicyApplied;
        if (items.isEmpty()) {
            masterPolicyApplied = "STANDARD_CANCELLATION_POLICY";
        } else {
            String firstPolicy = items.get(0).policyApplied();
            boolean allSame = items.stream().allMatch(item -> firstPolicy.equals(item.policyApplied()));
            masterPolicyApplied = allSame ? firstPolicy : "STANDARD_CANCELLATION_POLICY (Mixed items)";
        }

        return new CancellationPreviewResult(
                query.orderOrSubOrderId(),
                totalOriginalAmount,
                overallPercentage,
                totalRefundAmount,
                masterPolicyApplied,
                items
        );
    }
}
