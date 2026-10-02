package com.danasea.backend.modules.order.application.usecases;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.application.dtos.CreatePaymentIntentCommand;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.exceptions.UnauthorizedOrderAccessException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;

@Service
public class CreatePaymentIntentUseCase {

    private final MasterOrderRepositoryPort masterOrderRepository;
    private final PaymentGatewayPort paymentGatewayPort;

    public CreatePaymentIntentUseCase(
            MasterOrderRepositoryPort masterOrderRepository,
            PaymentGatewayPort paymentGatewayPort) {
        this.masterOrderRepository = masterOrderRepository;
        this.paymentGatewayPort = paymentGatewayPort;
    }

    @Transactional
    public PaymentIntentResult execute(CreatePaymentIntentCommand command) {
        if (command == null || command.customerId() == null || command.orderId() == null || command.provider() == null) {
            throw new IllegalArgumentException("Customer ID, Order ID and Payment Provider are required.");
        }
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required.");
        }

        MasterOrder order = masterOrderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // Chặn IDOR: Khách hàng chỉ được tạo intent cho đơn của chính mình
        if (!command.customerId().equals(order.getCustomerId())) {
            throw new UnauthorizedOrderAccessException(command.orderId(), command.customerId());
        }

        // Chỉ tạo payment intent cho đơn đang chờ thanh toán
        if (order.getStatus() != MasterOrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException(
                    "Payment intent can only be created for an order in PENDING_PAYMENT status; current: " + order.getStatus());
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderStateException("The order amount must be positive.");
        }

        return paymentGatewayPort.createPaymentIntent(order.getId(), order.getTotalAmount(), command.provider());
    }
}
