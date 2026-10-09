package com.danasea.backend.modules.ai.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.port.CustomerOrderReadPort.OwnedOrder;
import com.danasea.backend.modules.ai.application.port.CustomerOrderReadPort;
import com.danasea.backend.modules.ai.application.port.DecisionModelPort;
import com.danasea.backend.modules.ai.domain.models.DecisionResult;
import com.danasea.backend.modules.ai.domain.models.DecisionTask;
import com.danasea.backend.modules.order.application.dtos.CancellationPreviewResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerSupportUseCase {
    public record Result(String status, OwnedOrder order, CancellationPreviewResult cancellationPreview,
                         String previewStatus, DecisionResult classification, List<String> requiredInputs,
                         List<String> nextActions, boolean financialActionPerformed) {}
    private final CustomerOrderReadPort orders;
    private final DecisionModelPort decisions;

    public Result execute(UUID userId, UUID orderId, String message, boolean includeCancellationPreview) {
        if (userId == null) throw new IllegalArgumentException("An authenticated customer is required");
        if (message == null || message.isBlank() || message.length() > 1800) throw new IllegalArgumentException("Support message must contain 1 to 1800 characters");
        if (orderId == null) {
            return new Result("NEEDS_INPUT", null, null, "NOT_REQUESTED", decisions.decide(DecisionTask.COMPLAINT,
                    Map.of("complaint", message)), List.of("orderId"), List.of("SELECT_OWNED_ORDER"), false);
        }
        OwnedOrder owned = orders.read(userId, orderId);
        DecisionResult classification = decisions.decide(DecisionTask.COMPLAINT, Map.of("complaint", message,
                "order", Map.of("status", owned.status(), "paymentStatus", owned.paymentStatus())));
        CancellationPreviewResult preview = null;
        String previewStatus = "NOT_REQUESTED";
        if (includeCancellationPreview) {
            try {
                preview = orders.previewCancellation(userId, orderId);
                previewStatus = "CURRENT_POLICY_PREVIEW";
            } catch (IllegalArgumentException exception) { previewStatus = "UNAVAILABLE_FOR_CURRENT_STATE"; }
        }
        return new Result("AVAILABLE", owned, preview, previewStatus, classification, List.of(),
                List.of("CONTACT_SUPPORT", "REQUEST_CHANGE_WITH_CONFIRMATION", "USE_CANCELLATION_API_WITH_CONFIRMATION"), false);
    }
}
