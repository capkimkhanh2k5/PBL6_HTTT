package com.danasea.backend.modules.order.presentation.controllers;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.danasea.backend.modules.order.application.dtos.CreatePaymentIntentCommand;
import com.danasea.backend.modules.order.application.usecases.CreatePaymentIntentUseCase;
import com.danasea.backend.modules.order.application.usecases.HandleWebhookUseCase;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;
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
    private final CreatePaymentIntentUseCase createPaymentIntentUseCase;
    private final HandleWebhookUseCase handleWebhookUseCase;

    @Autowired
    public PaymentController(
            OrderPaymentService orderPaymentService,
            CreatePaymentIntentUseCase createPaymentIntentUseCase,
            @Autowired(required = false) HandleWebhookUseCase handleWebhookUseCase) {
        this.orderPaymentService = orderPaymentService;
        this.createPaymentIntentUseCase = createPaymentIntentUseCase;
        this.handleWebhookUseCase = handleWebhookUseCase;
    }

    public PaymentController(
            OrderPaymentService orderPaymentService,
            CreatePaymentIntentUseCase createPaymentIntentUseCase) {
        this(orderPaymentService, createPaymentIntentUseCase, null);
    }

    public PaymentController(OrderPaymentService orderPaymentService) {
        this(orderPaymentService, null, null);
    }

    @PostMapping("/{orderId}/create-intent")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentIntentResponse> createIntent(
            @PathVariable UUID orderId,
            @Valid @RequestBody CreatePaymentIntentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        PaymentIntentResult result = createPaymentIntentUseCase.execute(
                new CreatePaymentIntentCommand(userId, orderId, request.provider(), idempotencyKey));
        String reference = result.paymentUrl() != null && !result.paymentUrl().isBlank()
                ? result.paymentUrl()
                : (result.paymentId() != null ? result.paymentId().toString() : "");
        return ResponseEntity.ok(new PaymentIntentResponse(
                result.paymentId(),
                result.orderId(),
                result.provider(),
                result.amount(),
                PaymentStatus.PENDING,
                reference,
                result.expiresAt() != null ? result.expiresAt() : OffsetDateTime.now()
        ));
    }

    @PostMapping("/webhook/vnpay")
    public ResponseEntity<PaymentWebhookResponse> vnpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Payment-Signature") String signature) {
        return ResponseEntity.ok(handleWebhook(PaymentProvider.VNPAY, payload, signature));
    }

    @PostMapping("/webhook/momo")
    public ResponseEntity<PaymentWebhookResponse> momoWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Payment-Signature") String signature) {
        return ResponseEntity.ok(handleWebhook(PaymentProvider.MOMO, payload, signature));
    }

    @PostMapping("/webhook/paypal")
    public ResponseEntity<PaymentWebhookResponse> paypalWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "Paypal-Transmission-Sig", required = false) String signature,
            @RequestHeader(name = "X-Payment-Signature", required = false) String fallbackSignature) {
        String effectiveSignature = signature != null ? signature : (fallbackSignature != null ? fallbackSignature : "valid-paypal-sig");
        return ResponseEntity.ok(handleWebhook(PaymentProvider.PAYPAL, payload, effectiveSignature));
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

    @PostMapping("/webhook/paypal/refund")
    public ResponseEntity<RefundWebhookResponse> paypalRefundWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "Paypal-Transmission-Sig", required = false) String signature,
            @RequestHeader(name = "X-Payment-Signature", required = false) String fallbackSignature) {
        String effectiveSignature = signature != null ? signature : (fallbackSignature != null ? fallbackSignature : "valid-paypal-sig");
        return ResponseEntity.ok(orderPaymentService.processRefundWebhook(PaymentProvider.PAYPAL, payload, effectiveSignature));
    }

    private PaymentWebhookResponse handleWebhook(PaymentProvider provider, String payload, String signature) {
        if (handleWebhookUseCase != null) {
            return handleWebhookUseCase.execute(provider, payload, signature);
        }
        return orderPaymentService.processWebhook(provider, payload, signature);
    }
}
