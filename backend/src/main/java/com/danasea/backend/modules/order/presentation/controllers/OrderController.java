package com.danasea.backend.modules.order.presentation.controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.application.dtos.CreateOrderCommand;
import com.danasea.backend.modules.order.application.dtos.GetCustomerOrdersQuery;
import com.danasea.backend.modules.order.application.dtos.GetOrderDetailQuery;
import com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult;
import com.danasea.backend.modules.order.application.usecases.CreateOrderUseCase;
import com.danasea.backend.modules.order.application.usecases.GetCustomerOrdersUseCase;
import com.danasea.backend.modules.order.application.usecases.GetOrderDetailUseCase;
import com.danasea.backend.modules.order.application.usecases.RequestRefundUseCase;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.models.OrderPagedResult;
import com.danasea.backend.modules.order.domain.models.RefundEvaluationResult;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.order.presentation.dtos.CancellationPreviewResponse;
import com.danasea.backend.modules.order.presentation.dtos.CreateOrderRequest;
import com.danasea.backend.modules.order.presentation.dtos.OrderPageResponse;
import com.danasea.backend.modules.order.presentation.dtos.OrderResponse;
import com.danasea.backend.modules.order.presentation.dtos.RefundRequest;
import com.danasea.backend.modules.order.presentation.dtos.RefundResponse;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderCancellationPreview;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderResponse;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final JpaMasterOrderRepository masterOrderRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final JpaServiceSlotRepository serviceSlotRepository;
    private final RefundPolicyEngine refundPolicyEngine;
    private final OrderPaymentService orderPaymentService;
    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderDetailUseCase getOrderDetailUseCase;
    private final GetCustomerOrdersUseCase getCustomerOrdersUseCase;
    private final RequestRefundUseCase requestRefundUseCase;

    @Autowired
    public OrderController(
            JpaMasterOrderRepository masterOrderRepository,
            JpaSubOrderRepository subOrderRepository,
            JpaServiceSlotRepository serviceSlotRepository,
            RefundPolicyEngine refundPolicyEngine,
            OrderPaymentService orderPaymentService,
            CreateOrderUseCase createOrderUseCase,
            GetOrderDetailUseCase getOrderDetailUseCase,
            GetCustomerOrdersUseCase getCustomerOrdersUseCase,
            RequestRefundUseCase requestRefundUseCase) {
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.serviceSlotRepository = serviceSlotRepository;
        this.refundPolicyEngine = refundPolicyEngine;
        this.orderPaymentService = orderPaymentService;
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderDetailUseCase = getOrderDetailUseCase;
        this.getCustomerOrdersUseCase = getCustomerOrdersUseCase;
        this.requestRefundUseCase = requestRefundUseCase;
    }

    public OrderController(
            JpaMasterOrderRepository masterOrderRepository,
            JpaSubOrderRepository subOrderRepository,
            JpaServiceSlotRepository serviceSlotRepository,
            RefundPolicyEngine refundPolicyEngine,
            OrderPaymentService orderPaymentService) {
        this(masterOrderRepository, subOrderRepository, serviceSlotRepository, refundPolicyEngine,
                orderPaymentService, null, null, null, null);
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        UUID userId = currentUserId();
        if (createOrderUseCase != null) {
            MasterOrderDetailResult result = createOrderUseCase.execute(
                    new CreateOrderCommand(userId, request.bookingId(), idempotencyKey));
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                    .body(toOrderResponse(result));
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(orderPaymentService.createOrder(userId, request.bookingId(), idempotencyKey));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderPageResponse<OrderResponse>> getMyOrders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        if (getCustomerOrdersUseCase != null) {
            OrderPagedResult<MasterOrderDetailResult> paged = getCustomerOrdersUseCase.execute(
                    new GetCustomerOrdersQuery(currentUserId(), page, size));
            List<OrderResponse> content = paged.content().stream()
                    .map(this::toOrderResponse)
                    .toList();
            return ResponseEntity.ok(new OrderPageResponse<>(
                    content,
                    paged.page(),
                    paged.size(),
                    paged.totalElements(),
                    paged.totalPages()
            ));
        }
        return ResponseEntity.ok(orderPaymentService.getCustomerOrders(currentUserId(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable("id") UUID id) {
        if (getOrderDetailUseCase != null) {
            MasterOrderDetailResult result = getOrderDetailUseCase.execute(
                    new GetOrderDetailQuery(currentUserId(), id, isAdmin()));
            return ResponseEntity.ok(toOrderResponse(result));
        }
        return ResponseEntity.ok(orderPaymentService.getOrder(currentUserId(), id, isAdmin()));
    }

    @PostMapping("/{id}/refund-request")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<RefundResponse>> requestRefund(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) RefundRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        RefundReason reason = request == null ? RefundReason.CUSTOMER_REQUEST : request.reason();
        return ResponseEntity.status(org.springframework.http.HttpStatus.ACCEPTED)
                .body(orderPaymentService.requestRefund(currentUserId(), id, reason, idempotencyKey));
    }

    @GetMapping("/{id}/cancellation-preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CancellationPreviewResponse> getCancellationPreview(@PathVariable("id") UUID id) {
        UUID currentUserId = currentUserId();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        MasterOrderJpaEntity masterOrder;
        List<SubOrderJpaEntity> subOrders;

        var masterOrderOpt = masterOrderRepository.findById(id);
        if (masterOrderOpt.isPresent()) {
            masterOrder = masterOrderOpt.get();
            subOrders = subOrderRepository.findByMasterOrderId(id);
        } else {
            var subOrderOpt = subOrderRepository.findById(id);
            if (subOrderOpt.isPresent()) {
                SubOrderJpaEntity subOrder = subOrderOpt.get();
                masterOrder = masterOrderRepository.findById(subOrder.getMasterOrderId())
                        .orElseThrow(() -> new OrderNotFoundException("Master order not found for sub-order: " + id));
                subOrders = List.of(subOrder);
            } else {
                throw new OrderNotFoundException("Order not found with id: " + id);
            }
        }

        // IDOR security check
        if (!isAdmin && (masterOrder.getCustomerId() == null || !masterOrder.getCustomerId().equals(currentUserId))) {
            throw new AccessDeniedException("User does not have permission to view cancellation preview for this order");
        }

        LocalDateTime now = LocalDateTime.now();
        List<SubOrderCancellationPreview> items = new ArrayList<>();
        BigDecimal totalOriginalAmount = BigDecimal.ZERO;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (SubOrderJpaEntity subOrder : subOrders) {
            LocalDateTime departureTime = null;
            if (subOrder.getSlotId() != null) {
                var slotOpt = serviceSlotRepository.findById(subOrder.getSlotId());
                if (slotOpt.isPresent()) {
                    ServiceSlotJpaEntity slot = slotOpt.get();
                    if (slot.getDate() != null && slot.getStartTime() != null) {
                        departureTime = LocalDateTime.of(slot.getDate(), slot.getStartTime());
                    }
                }
            }

            BigDecimal originalAmount = subOrder.getSubtotalAmount() != null
                    ? subOrder.getSubtotalAmount()
                    : BigDecimal.ZERO;

            RefundEvaluationResult eval = refundPolicyEngine.evaluate(
                    RefundReason.CUSTOMER_REQUEST,
                    departureTime,
                    now,
                    originalAmount
            );

            String policyApplied = refundPolicyEngine.getPolicyDescription(RefundReason.CUSTOMER_REQUEST, eval.refundPercentage());

            items.add(new SubOrderCancellationPreview(
                    subOrder.getId(),
                    subOrder.getServiceId(),
                    subOrder.getSlotId(),
                    departureTime,
                    originalAmount,
                    eval.refundPercentage(),
                    eval.refundAmount(),
                    policyApplied
            ));

            totalOriginalAmount = totalOriginalAmount.add(originalAmount);
            totalRefundAmount = totalRefundAmount.add(eval.refundAmount());
        }

        BigDecimal overallPercentage;
        if (totalOriginalAmount.compareTo(BigDecimal.ZERO) > 0) {
            overallPercentage = totalRefundAmount.multiply(BigDecimal.valueOf(100.0))
                    .divide(totalOriginalAmount, 1, RoundingMode.HALF_UP);
        } else {
            overallPercentage = BigDecimal.valueOf(0.0);
        }

        String masterPolicyApplied;
        if (items.isEmpty()) {
            masterPolicyApplied = "STANDARD_CANCELLATION_POLICY";
        } else {
            String firstPolicy = items.get(0).policyApplied();
            boolean allSame = items.stream().allMatch(item -> firstPolicy.equals(item.policyApplied()));
            masterPolicyApplied = allSame ? firstPolicy : "STANDARD_CANCELLATION_POLICY (Mixed items)";
        }

        CancellationPreviewResponse response = new CancellationPreviewResponse(
                id,
                totalOriginalAmount,
                overallPercentage,
                totalRefundAmount,
                masterPolicyApplied,
                items
        );

        return ResponseEntity.ok(response);
    }

    private OrderResponse toOrderResponse(MasterOrderDetailResult r) {
        if (r == null) {
            return null;
        }
        List<SubOrderResponse> items = r.subOrders() == null ? List.of() : r.subOrders().stream()
                .map(s -> new SubOrderResponse(
                        s.id(),
                        s.vendorId(),
                        s.serviceId(),
                        s.slotId(),
                        s.quantity(),
                        s.unitPrice(),
                        s.subtotalAmount(),
                        s.status()
                )).toList();
        return new OrderResponse(
                r.id(),
                r.bookingId(),
                r.customerId(),
                r.status(),
                r.totalAmount(),
                r.discountAmount(),
                items,
                r.createdAt(),
                r.createdAt()
        );
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
