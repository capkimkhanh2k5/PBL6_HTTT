package com.danasea.backend.modules.ai.infrastructure.adapters;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.ai.application.port.TransactionRiskReadPort;
import com.danasea.backend.modules.order.application.api.AiOrderRiskReadApi;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionRiskReadAdapter implements TransactionRiskReadPort {
    private final AiOrderRiskReadApi orders;

    @Override public Facts read(UUID orderId) {
        var facts = orders.read(orderId);
        return new Facts(facts.orderId(), facts.customerId(), facts.failedPayments24h(), facts.orders1h(),
                facts.refundRequests7d(), facts.paymentStatus());
    }
}
