package com.danasea.backend.modules.order.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.application.dtos.HandleWebhookCommand;
import com.danasea.backend.modules.order.application.dtos.WebhookProcessResult;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.exceptions.PaymentVerificationException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.ports.BookingStatusUpdatePort;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.OrderEventPublisherPort;
import com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;

@Service
public class HandleWebhookUseCase {

    private final MasterOrderRepositoryPort masterOrderRepository;
    private final SubOrderRepositoryPort subOrderRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final BookingStatusUpdatePort bookingStatusUpdatePort;
    private final OrderEventPublisherPort orderEventPublisherPort;

    public HandleWebhookUseCase(
            MasterOrderRepositoryPort masterOrderRepository,
            SubOrderRepositoryPort subOrderRepository,
            PaymentGatewayPort paymentGatewayPort,
            BookingStatusUpdatePort bookingStatusUpdatePort,
            OrderEventPublisherPort orderEventPublisherPort) {
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.paymentGatewayPort = paymentGatewayPort;
        this.bookingStatusUpdatePort = bookingStatusUpdatePort;
        this.orderEventPublisherPort = orderEventPublisherPort;
    }

    @Transactional
    public WebhookProcessResult execute(HandleWebhookCommand command, MasterOrder order) {
        if (command == null || command.provider() == null) {
            throw new IllegalArgumentException("Webhook command and provider are required.");
        }

        // 1. Xác thực chữ ký số HMAC-SHA512 (Mục 9.2.4 & 9.2.14)
        boolean validSignature = paymentGatewayPort.verifyWebhookSignature(command.rawParams(), command.signature());
        if (!validSignature) {
            throw new PaymentVerificationException("Invalid webhook signature for provider: " + command.provider());
        }

        if (order == null) {
            throw new OrderNotFoundException("Order associated with payment webhook not found.");
        }

        // 2. Tự guard: Kiểm tra trạng thái hiện tại của Order
        if (PaymentStatus.SUCCESS.equals(command.status())) {
            if (MasterOrderStatus.PAID.equals(order.getStatus())) {
                // Đã xử lý trước đó -> Idempotent no-op (200 OK)
                return new WebhookProcessResult(command.eventId(), command.paymentId(), command.status(), true);
            }

            if (!MasterOrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
                throw new InvalidOrderStateException(
                        "Cannot apply payment success to order in status: " + order.getStatus());
            }

            // 3. Chuyển trạng thái Order sang PAID và SubOrders sang CONFIRMED
            List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(order.getId());
            order.setSubOrders(subOrders);
            order.markPaid();

            masterOrderRepository.save(order);
            subOrderRepository.saveAll(order.getSubOrders());

            // 4. Kích hoạt xác nhận Booking tương ứng qua Port
            bookingStatusUpdatePort.confirmBooking(order.getBookingId(), order.getCustomerId());

            // 5. Bắn sự kiện Payment Success
            orderEventPublisherPort.publishPaymentSuccessEvent(order);
        }

        return new WebhookProcessResult(command.eventId(), command.paymentId(), command.status(), false);
    }
}
