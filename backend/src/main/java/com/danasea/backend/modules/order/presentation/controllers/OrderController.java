package com.danasea.backend.modules.order.presentation.controllers;

import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.models.RefundEvaluationResult;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.order.presentation.dtos.CancellationPreviewResponse;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderCancellationPreview;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import com.danasea.backend.shared.i18n.LocalizedMessageService;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final JpaMasterOrderRepository masterOrderRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final JpaServiceSlotRepository serviceSlotRepository;
    private final RefundPolicyEngine refundPolicyEngine;

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.standalone();

    @GetMapping("/{id}/cancellation-preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CancellationPreviewResponse> getCancellationPreview(@PathVariable("id") UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        MasterOrderJpaEntity masterOrder;
        List<SubOrderJpaEntity> subOrders;

        var masterOrderOpt = masterOrderRepository.findById(id);
        if (masterOrderOpt.isPresent()) {
            masterOrder = masterOrderOpt.get();
            subOrders = subOrderRepository.findByMasterOrderId(id);
        } else {
            var subOrderOpt = subOrderRepository.findById(id);
            if (subOrderOpt.isPresent()) {
                SubOrderJpaEntity subOrder = subOrderOpt.get();
                masterOrder = masterOrderRepository.findById(subOrder.getMasterOrderId())
                        .orElseThrow(() -> new OrderNotFoundException("Master order not found for sub-order: " + id));
                subOrders = List.of(subOrder);
            } else {
                throw new OrderNotFoundException("Order not found with id: " + id);
            }
        }

        // IDOR security check
        if (!isAdmin && (masterOrder.getCustomerId() == null || !masterOrder.getCustomerId().equals(currentUserId))) {
            throw new AccessDeniedException("User does not have permission to view cancellation preview for this order");
        }

        LocalDateTime now = LocalDateTime.now();
        List<SubOrderCancellationPreview> items = new ArrayList<>();
        BigDecimal totalOriginalAmount = BigDecimal.ZERO;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (SubOrderJpaEntity subOrder : subOrders) {
            LocalDateTime departureTime = null;
            if (subOrder.getSlotId() != null) {
                var slotOpt = serviceSlotRepository.findById(subOrder.getSlotId());
                if (slotOpt.isPresent()) {
                    ServiceSlotJpaEntity slot = slotOpt.get();
                    if (slot.getDate() != null && slot.getStartTime() != null) {
                        departureTime = LocalDateTime.of(slot.getDate(), slot.getStartTime());
                    }
                }
            }

            BigDecimal originalAmount = subOrder.getSubtotalAmount() != null
                    ? subOrder.getSubtotalAmount()
                    : BigDecimal.ZERO;

            RefundEvaluationResult eval = refundPolicyEngine.evaluate(
                    RefundReason.CUSTOMER_REQUEST,
                    departureTime,
                    now,
                    originalAmount
            );

            String policyApplied = messages.get("policy.refund." + eval.policyCode().toLowerCase());

            items.add(new SubOrderCancellationPreview(
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

        CancellationPreviewResponse response = new CancellationPreviewResponse(
                id,
                totalOriginalAmount,
                overallPercentage,
                totalRefundAmount,
                masterPolicyApplied,
                items
        );

        return ResponseEntity.ok(response);
    }
}
