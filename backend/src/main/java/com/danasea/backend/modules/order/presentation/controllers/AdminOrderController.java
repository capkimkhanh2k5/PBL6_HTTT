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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;
import com.danasea.backend.modules.order.presentation.dtos.AdminOrderSummaryResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderPaymentService orderPaymentService;

    @GetMapping
    public ResponseEntity<Page<AdminOrderSummaryResponse>> getOrders(
            @RequestParam(name = "status", required = false) MasterOrderStatus status,
            @RequestParam(name = "paymentStatus", required = false) PaymentOrderStatus paymentStatus,
            @RequestParam(name = "vendorId", required = false) UUID vendorId,
            @RequestParam(name = "customerId", required = false) UUID customerId,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        OffsetDateTime effectiveFrom = fromDate != null ? fromDate : from;
        OffsetDateTime effectiveTo = toDate != null ? toDate : to;
        log.info("Admin query orders with status: {}, paymentStatus: {}, vendorId: {}, customerId: {}, from: {}, to: {}",
                status, paymentStatus, vendorId, customerId, effectiveFrom, effectiveTo);
        Page<AdminOrderSummaryResponse> response = orderPaymentService.getAdminOrders(
                status, paymentStatus, customerId, vendorId, effectiveFrom, effectiveTo, pageable);
        return ResponseEntity.ok(response);
    }
}
