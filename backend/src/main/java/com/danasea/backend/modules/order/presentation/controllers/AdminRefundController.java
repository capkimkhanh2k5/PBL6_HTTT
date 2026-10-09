package com.danasea.backend.modules.order.presentation.controllers;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
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
            @RequestParam(name = "orderId", required = false) UUID orderId,
            @RequestParam(name = "provider", required = false) PaymentProvider provider,
            @RequestParam(name = "vendorId", required = false) UUID vendorId,
            @RequestParam(name = "customerId", required = false) UUID customerId,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        OffsetDateTime effectiveFrom = fromDate != null ? fromDate : from;
        OffsetDateTime effectiveTo = toDate != null ? toDate : to;
        log.info("Admin query refunds with status: {}, reason: {}, subOrderId: {}, orderId: {}, provider: {}, vendorId: {}, customerId: {}, from: {}, to: {}",
                status, reason, subOrderId, orderId, provider, vendorId, customerId, effectiveFrom, effectiveTo);
        Page<RefundDetailResponse> response = (orderId == null && provider == null && vendorId == null && customerId == null && effectiveFrom == null && effectiveTo == null)
                ? orderPaymentService.getAdminRefunds(status, reason, subOrderId, pageable)
                : orderPaymentService.getAdminRefunds(status, reason, subOrderId, provider, vendorId, customerId, orderId, effectiveFrom, effectiveTo, pageable);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Page<RefundDetailResponse>> getRefunds(
            RefundStatus status,
            RefundReason reason,
            UUID subOrderId,
            Pageable pageable) {
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
