package com.danasea.backend.modules.order.presentation.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.presentation.dtos.CreatePaymentIntentRequest;
import com.danasea.backend.modules.order.presentation.dtos.PaymentIntentResponse;
import com.danasea.backend.modules.order.presentation.dtos.PaymentWebhookResponse;
import com.danasea.backend.modules.order.presentation.dtos.RefundWebhookResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final OrderPaymentService orderPaymentService;

    public PaymentController(OrderPaymentService orderPaymentService) {
        this.orderPaymentService = orderPaymentService;
    }

    @PostMapping("/{orderId}/create-intent")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentIntentResponse> createIntent(
            @PathVariable UUID orderId,
            @Valid @RequestBody CreatePaymentIntentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));
        return ResponseEntity.ok(orderPaymentService.createPaymentIntent(
                userId, orderId, request.provider(), idempotencyKey));
    }

    @PostMapping("/webhook/vnpay")
    public ResponseEntity<PaymentWebhookResponse> vnpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Payment-Signature") String signature) {
        return ResponseEntity.ok(orderPaymentService.processWebhook(PaymentProvider.VNPAY, payload, signature));
    }

    @PostMapping("/webhook/momo")
    public ResponseEntity<PaymentWebhookResponse> momoWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Payment-Signature") String signature) {
        return ResponseEntity.ok(orderPaymentService.processWebhook(PaymentProvider.MOMO, payload, signature));
    }

    @PostMapping("/webhook/sepay")
    public ResponseEntity<PaymentWebhookResponse> sepayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Payment-Signature") String signature) {
        return ResponseEntity.ok(orderPaymentService.processWebhook(PaymentProvider.SEPAY, payload, signature));
    }

    @PostMapping("/webhook/vnpay/refund")
    public ResponseEntity<RefundWebhookResponse> vnpayRefundWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Payment-Signature") String signature) {
        return ResponseEntity.ok(orderPaymentService.processRefundWebhook(PaymentProvider.VNPAY, payload, signature));
    }

    @PostMapping("/webhook/momo/refund")
    public ResponseEntity<RefundWebhookResponse> momoRefundWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Payment-Signature") String signature) {
        return ResponseEntity.ok(orderPaymentService.processRefundWebhook(PaymentProvider.MOMO, payload, signature));
    }

    @PostMapping("/webhook/sepay/refund")
    public ResponseEntity<RefundWebhookResponse> sepayRefundWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Payment-Signature") String signature) {
        return ResponseEntity.ok(orderPaymentService.processRefundWebhook(PaymentProvider.SEPAY, payload, signature));
    }
}
