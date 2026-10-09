package com.danasea.backend.modules.ai.infrastructure.adapters;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.ai.application.port.CustomerOrderReadPort;
import com.danasea.backend.modules.order.application.dtos.CancellationPreviewResult;
import com.danasea.backend.modules.order.application.dtos.GetCancellationPreviewQuery;
import com.danasea.backend.modules.order.application.dtos.GetOrderDetailQuery;
import com.danasea.backend.modules.order.application.usecases.GetCancellationPreviewUseCase;
import com.danasea.backend.modules.order.application.usecases.GetOrderDetailUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerOrderReadAdapter implements CustomerOrderReadPort {
    private final GetOrderDetailUseCase orders;
    private final GetCancellationPreviewUseCase cancellations;

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
}
