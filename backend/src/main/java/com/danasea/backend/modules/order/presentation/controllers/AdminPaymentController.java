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
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.presentation.dtos.PaymentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final OrderPaymentService orderPaymentService;

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @RequestParam(name = "status", required = false) PaymentStatus status,
            @RequestParam(name = "provider", required = false) PaymentProvider provider,
            @RequestParam(name = "orderId", required = false) UUID orderId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin query payments with status: {}, provider: {}, orderId: {}", status, provider, orderId);
        Page<PaymentResponse> response = orderPaymentService.getAdminPayments(status, provider, orderId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentDetail(@PathVariable("id") UUID paymentId) {
        log.info("Admin query payment detail with id: {}", paymentId);
        PaymentResponse response = orderPaymentService.getAdminPaymentDetail(paymentId);
        return ResponseEntity.ok(response);
    }
}
