package com.danasea.backend.modules.ai.application.usecases;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.ports.AssessmentCaseStorePort;
import com.danasea.backend.modules.ai.application.ports.DecisionModelPort;
import com.danasea.backend.modules.ai.application.ports.TransactionRiskReadPort;
import com.danasea.backend.modules.ai.domain.models.AssessmentCase;
import com.danasea.backend.modules.ai.domain.models.DecisionTask;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyzeTransactionRiskUseCase {
    private final TransactionRiskReadPort facts;
    private final DecisionModelPort model;
    private final AssessmentCaseStorePort cases;

    public AssessmentCase execute(UUID adminId, UUID orderId, String complaint) {
        if (adminId == null || orderId == null || complaint != null && complaint.length() > 1800) {
            throw new IllegalArgumentException("Invalid transaction risk request");
        }
        var observed = facts.read(orderId);
        List<String> signals = new ArrayList<>();
        if (observed.failedPayments24h() >= 3) signals.add("THREE_FAILED_PAYMENTS_IN_24H");
        if (observed.orders1h() >= 5) signals.add("FIVE_ORDERS_IN_1H");
        if (observed.refundRequests7d() >= 3) signals.add("THREE_REFUND_REQUESTS_IN_7D");
        var decision = model.decide(DecisionTask.RISK, Map.of("risk_signals", signals, "complaint", complaint == null ? "" : complaint,
                "payment_status", observed.paymentStatus()));
        Double probability = decision.probability("needs_review");
        String status = !signals.isEmpty() || !decision.available() || probability != null && probability >= 0.5
                ? "NEEDS_REVIEW" : "NO_RULE_SIGNAL";
        return cases.create("TRANSACTION_RISK", orderId, adminId, status,
                Map.of("observed", observed, "riskSignals", signals, "complaint", complaint == null ? "" : complaint,
                        "ruleVersion", "velocity-demo-v1", "fraudConfirmed", false), Map.of("risk", decision));
    }
}
