package com.danasea.backend.modules.order.application.usecases;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.application.dtos.OrderRefundResult;
import com.danasea.backend.modules.order.application.dtos.RequestRefundCommand;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.exceptions.UnauthorizedOrderAccessException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.RefundEvaluationResult;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;

@Service
public class RequestRefundUseCase {

    private final MasterOrderRepositoryPort masterOrderRepository;
    private final SubOrderRepositoryPort subOrderRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final RefundPolicyEngine refundPolicyEngine;

    public RequestRefundUseCase(
            MasterOrderRepositoryPort masterOrderRepository,
            SubOrderRepositoryPort subOrderRepository,
            PaymentGatewayPort paymentGatewayPort,
            RefundPolicyEngine refundPolicyEngine) {
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.paymentGatewayPort = paymentGatewayPort;
        this.refundPolicyEngine = refundPolicyEngine;
    }

    @Transactional
    public List<OrderRefundResult> execute(RequestRefundCommand command, LocalDateTime departureTime) {
        if (command == null || command.customerId() == null || command.orderOrSubOrderId() == null) {
            throw new IllegalArgumentException("Customer ID and Order ID are required.");
        }
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required.");
        }

        RefundReason reason = command.requestedReason() == null
                ? RefundReason.CUSTOMER_REQUEST
                : command.requestedReason();

        if (reason != RefundReason.CUSTOMER_REQUEST && reason != RefundReason.CUSTOMER_CANCEL) {
            throw new IllegalArgumentException("Customers may only request a customer cancellation refund.");
        }

        MasterOrder order;
        List<SubOrder> subOrders;

        Optional<MasterOrder> masterOpt = masterOrderRepository.findById(command.orderOrSubOrderId());
        if (masterOpt.isPresent()) {
            order = masterOpt.get();
            subOrders = subOrderRepository.findByMasterOrderId(order.getId());
        } else {
            SubOrder subOrder = subOrderRepository.findById(command.orderOrSubOrderId())
                    .orElseThrow(() -> new OrderNotFoundException(command.orderOrSubOrderId()));
            order = masterOrderRepository.findById(subOrder.getMasterOrderId())
                    .orElseThrow(() -> new OrderNotFoundException(subOrder.getMasterOrderId()));
            subOrders = List.of(subOrder);
        }

        // Chặn IDOR: Khách hàng chỉ được yêu cầu hoàn tiền cho đơn của chính mình
        if (!command.customerId().equals(order.getCustomerId())) {
            throw new UnauthorizedOrderAccessException(order.getId(), command.customerId());
        }

        // Chỉ cho phép yêu cầu hoàn tiền khi đơn đã thanh toán
        if (order.getStatus() != MasterOrderStatus.PAID
                && order.getStatus() != MasterOrderStatus.PARTIALLY_COMPLETED
                && order.getStatus() != MasterOrderStatus.COMPLETED) {
            throw new InvalidOrderStateException("Refunds can only be requested for a paid order; current: " + order.getStatus());
        }

        LocalDateTime cancelTime = command.cancelTime() != null ? command.cancelTime() : LocalDateTime.now();
        List<OrderRefundResult> results = new ArrayList<>();

        for (SubOrder subOrder : subOrders) {
            if (subOrder.getStatus() == SubOrderStatus.CANCELLED
                    || subOrder.getStatus() == SubOrderStatus.REFUNDED
                    || subOrder.getStatus() == SubOrderStatus.REJECTED) {
                throw new InvalidOrderStateException("Sub-order " + subOrder.getId() + " is already in terminal state: " + subOrder.getStatus());
            }

            RefundEvaluationResult evaluation = refundPolicyEngine.evaluate(
                    reason,
                    departureTime,
                    cancelTime,
                    subOrder.getSubtotalAmount()
            );

            if (evaluation.refundAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidOrderStateException(
                        "The cancellation policy does not allow a refund for sub-order " + subOrder.getId() + " (< 24h).");
            }

            // Gọi cổng thanh toán để thực hiện lệnh refund
            paymentGatewayPort.requestRefund(subOrder.getId().toString(), evaluation.refundAmount());

            results.add(new OrderRefundResult(
                    UUID.randomUUID(),
                    subOrder.getId(),
                    evaluation.refundAmount(),
                    evaluation.refundPercentage(),
                    reason,
                    RefundStatus.PENDING,
                    evaluation.policyCode()
            ));
        }

        return results;
    }
}
