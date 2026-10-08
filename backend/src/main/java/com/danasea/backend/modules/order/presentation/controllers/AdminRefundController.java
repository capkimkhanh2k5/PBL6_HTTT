package com.danasea.backend.modules.order.presentation.controllers;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.presentation.dtos.RefundDetailResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/refunds")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRefundController {

    private final OrderPaymentService orderPaymentService;

    @GetMapping
    public ResponseEntity<Page<RefundDetailResponse>> getRefunds(
            @RequestParam(name = "status", required = false) RefundStatus status,
            @RequestParam(name = "reason", required = false) RefundReason reason,
            @RequestParam(name = "subOrderId", required = false) UUID subOrderId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin query refunds with status: {}, reason: {}, subOrderId: {}", status, reason, subOrderId);
        Page<RefundDetailResponse> response = orderPaymentService.getAdminRefunds(status, reason, subOrderId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundDetailResponse> getRefundDetail(@PathVariable("id") UUID refundId) {
        log.info("Admin query refund detail with id: {}", refundId);
        RefundDetailResponse response = orderPaymentService.getAdminRefundDetail(refundId);
        return ResponseEntity.ok(response);
    }
}
