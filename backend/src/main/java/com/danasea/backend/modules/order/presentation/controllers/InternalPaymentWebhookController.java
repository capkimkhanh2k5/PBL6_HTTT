package com.danasea.backend.modules.order.presentation.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.presentation.dtos.PaymentWebhookResponse;

import lombok.RequiredArgsConstructor;

@Profile("test")
@RestController
@RequestMapping("/api/payments/webhook/internal")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class InternalPaymentWebhookController {
    private final OrderPaymentService orderPaymentService;

    @PostMapping("/{provider}")
    public ResponseEntity<PaymentWebhookResponse> webhook(@PathVariable PaymentProvider provider,
            @RequestBody String payload, @RequestHeader("X-Payment-Signature") String signature) {
        return ResponseEntity.ok(orderPaymentService.processWebhook(provider, payload, signature));
    }
}
