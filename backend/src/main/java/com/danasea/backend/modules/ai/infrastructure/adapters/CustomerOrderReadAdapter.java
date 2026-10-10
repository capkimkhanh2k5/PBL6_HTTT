package com.danasea.backend.modules.ai.infrastructure.adapters;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.ai.application.ports.CustomerOrderReadPort;
import com.danasea.backend.modules.order.application.dtos.CancellationPreviewResult;
import com.danasea.backend.modules.order.application.dtos.GetCancellationPreviewQuery;
import com.danasea.backend.modules.order.application.dtos.GetOrderDetailQuery;
import com.danasea.backend.modules.order.application.usecases.GetCancellationPreviewUseCase;
import com.danasea.backend.modules.order.application.usecases.GetOrderDetailUseCase;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.services.CustomerRefundEligibilityPolicy;
import com.danasea.backend.modules.service.application.api.CustomerChangeSlotReadApi;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerOrderReadAdapter implements CustomerOrderReadPort {
    private final GetOrderDetailUseCase orders;
    private final GetCancellationPreviewUseCase cancellations;
    private final CustomerChangeSlotReadApi changeSlots;

    @Override
    public OwnedOrder read(UUID userId, UUID orderId) {
        var order = orders.execute(new GetOrderDetailQuery(userId, orderId, false));
        return new OwnedOrder(order.id(), order.status().name(), order.paymentStatus() == null ? "UNKNOWN" : order.paymentStatus().name(),
                order.totalAmount(), order.subOrders().stream().map(item -> new Item(item.id(), item.serviceId(),
                        item.quantity() == null ? 0 : item.quantity(), item.unitPrice(), item.subtotalAmount(), item.status().name())).toList());
    }

    @Override
    public CancellationPreviewResult previewCancellation(UUID userId, UUID orderId) {
        return cancellations.execute(new GetCancellationPreviewQuery(userId, orderId, false));
    }
    @Override
    public void validateChangeTarget(UUID userId, UUID orderId, UUID slotId, LocalDate requestedDate) {
        if (slotId == null) return;
        var order = orders.execute(new GetOrderDetailQuery(userId, orderId, false));
        var target = changeSlots.read(slotId);
        if (order.subOrders().stream().noneMatch(item -> target.serviceId().equals(item.serviceId()) && (item.status() == SubOrderStatus.PENDING || item.status() == SubOrderStatus.CONFIRMED))) throw new IllegalArgumentException("Requested slot must belong to an owned order service");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (!"OPEN".equals(target.status()) || target.date() == null || target.startTime() == null || target.date().isBefore(today)
                || target.date().isAfter(today.plusDays(90)) || target.date().equals(today) && !target.startTime().isAfter(LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))) {
            throw new IllegalArgumentException("Requested slot is not open for a future date");
        }
        if (requestedDate != null && !requestedDate.equals(target.date())) throw new IllegalArgumentException("Requested date must match the requested slot");
    }
    @Override
    public Eligibility eligibility(UUID userId, UUID orderId, CancellationPreviewResult preview) {
        var order = orders.execute(new GetOrderDetailQuery(userId, orderId, false));
        boolean paid = CustomerRefundEligibilityPolicy.paidOrder(order.status());
        boolean validItems = !order.subOrders().isEmpty() && order.subOrders().stream()
                .allMatch(item -> CustomerRefundEligibilityPolicy.refundableItem(item.status()));
        boolean positivePolicy = preview != null && preview.items() != null && !preview.items().isEmpty()
                && preview.items().stream().allMatch(item -> item.refundAmount() != null && item.refundAmount().signum() > 0);
        boolean change = order.status() != MasterOrderStatus.CANCELLED && order.status() != MasterOrderStatus.COMPLETED
                && order.subOrders().stream().anyMatch(item -> item.status() == SubOrderStatus.PENDING || item.status() == SubOrderStatus.CONFIRMED);
        List<String> reasons = new ArrayList<>();
        if (!paid) reasons.add("ORDER_NOT_PAID");
        if (!validItems) reasons.add("ITEM_STATE_REQUIRES_SUPPORT_REVIEW");
        if (!positivePolicy) reasons.add("POSITIVE_REFUND_PREVIEW_REQUIRED");
        return new Eligibility(paid && validItems && positivePolicy, change, "CORE_REFUND_ELIGIBILITY_POLICY_AND_CURRENT_CANCELLATION_PREVIEW", List.copyOf(reasons));
    }
}
