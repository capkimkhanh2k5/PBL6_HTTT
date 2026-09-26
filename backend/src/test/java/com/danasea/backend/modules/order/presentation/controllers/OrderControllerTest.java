package com.danasea.backend.modules.order.presentation.controllers;

import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.order.presentation.dtos.CancellationPreviewResponse;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Unit Tests")
class OrderControllerTest {

    @Mock
    private JpaMasterOrderRepository masterOrderRepository;

    @Mock
    private JpaSubOrderRepository subOrderRepository;

    @Mock
    private JpaServiceSlotRepository serviceSlotRepository;

    @Spy
    private RefundPolicyEngine refundPolicyEngine = new RefundPolicyEngine();

    @Mock
    private com.danasea.backend.modules.order.application.usecases.CreateOrderUseCase createOrderUseCase;

    @Mock
    private com.danasea.backend.modules.order.application.usecases.GetOrderDetailUseCase getOrderDetailUseCase;

    @Mock
    private com.danasea.backend.modules.order.application.usecases.GetCustomerOrdersUseCase getCustomerOrdersUseCase;

    @InjectMocks
    private OrderController orderController;

    private UUID customerId;
    private UUID otherCustomerId;
    private UUID masterOrderId;
    private UUID subOrderId;
    private UUID slotId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        otherCustomerId = UUID.randomUUID();
        masterOrderId = UUID.randomUUID();
        subOrderId = UUID.randomUUID();
        slotId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityUser(UUID userId, String... roles) {
        var authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(userId.toString(), "credentials", authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("Owner previews cancellation by MasterOrder ID -> 200 OK with aggregated items")
    void preview_ByMasterOrderId_AsOwner_Success200() {
        mockSecurityUser(customerId, "ROLE_CUSTOMER");

        MasterOrderJpaEntity masterOrder = new MasterOrderJpaEntity();
        masterOrder.setId(masterOrderId);
        masterOrder.setCustomerId(customerId);
        masterOrder.setTotalAmount(new BigDecimal("2000000.00"));

        SubOrderJpaEntity subOrder1 = new SubOrderJpaEntity();
        subOrder1.setId(UUID.randomUUID());
        subOrder1.setMasterOrderId(masterOrderId);
        subOrder1.setSlotId(slotId);
        subOrder1.setSubtotalAmount(new BigDecimal("1000000.00"));

        LocalDate futureDate = LocalDate.now().plusDays(5); // > 48h -> 100%
        ServiceSlotJpaEntity slot1 = new ServiceSlotJpaEntity();
        slot1.setId(slotId);
        slot1.setDate(futureDate);
        slot1.setStartTime(LocalTime.of(10, 0));

        when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));
        when(subOrderRepository.findByMasterOrderId(masterOrderId)).thenReturn(List.of(subOrder1));
        when(serviceSlotRepository.findById(slotId)).thenReturn(Optional.of(slot1));

        ResponseEntity<CancellationPreviewResponse> response = orderController.getCancellationPreview(masterOrderId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        CancellationPreviewResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(masterOrderId, body.orderId());
        assertEquals(new BigDecimal("1000000.00"), body.originalAmount());
        assertEquals(BigDecimal.valueOf(100.0), body.refundPercentage());
        assertEquals(new BigDecimal("1000000.00"), body.refundAmount());
        assertEquals(1, body.items().size());
    }

    @Test
    @DisplayName("Owner previews cancellation by SubOrder ID -> 200 OK polymorphic resolution")
    void preview_BySubOrderId_AsOwner_Success200() {
        mockSecurityUser(customerId, "ROLE_CUSTOMER");

        MasterOrderJpaEntity masterOrder = new MasterOrderJpaEntity();
        masterOrder.setId(masterOrderId);
        masterOrder.setCustomerId(customerId);

        SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
        subOrder.setId(subOrderId);
        subOrder.setMasterOrderId(masterOrderId);
        subOrder.setSlotId(slotId);
        subOrder.setSubtotalAmount(new BigDecimal("1000000.00"));

        LocalDate tomorrow = LocalDate.now().plusDays(1); // within 24h-48h or 2h-24h
        ServiceSlotJpaEntity slot = new ServiceSlotJpaEntity();
        slot.setId(slotId);
        slot.setDate(tomorrow);
        slot.setStartTime(LocalTime.of(12, 0));

        when(masterOrderRepository.findById(subOrderId)).thenReturn(Optional.empty());
        when(subOrderRepository.findById(subOrderId)).thenReturn(Optional.of(subOrder));
        when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));
        when(serviceSlotRepository.findById(slotId)).thenReturn(Optional.of(slot));

        ResponseEntity<CancellationPreviewResponse> response = orderController.getCancellationPreview(subOrderId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        CancellationPreviewResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(subOrderId, body.orderId());
        assertEquals(1, body.items().size());
        assertEquals(new BigDecimal("1000000.00"), body.originalAmount());
    }

    @Test
    @DisplayName("Admin previews cancellation for any customer order -> 200 OK bypasses IDOR")
    void preview_AsAdmin_Success200() {
        mockSecurityUser(UUID.randomUUID(), "ROLE_ADMIN");

        MasterOrderJpaEntity masterOrder = new MasterOrderJpaEntity();
        masterOrder.setId(masterOrderId);
        masterOrder.setCustomerId(customerId); // different user

        when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));
        when(subOrderRepository.findByMasterOrderId(masterOrderId)).thenReturn(List.of());

        ResponseEntity<CancellationPreviewResponse> response = orderController.getCancellationPreview(masterOrderId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Unauthorized customer accesses another customer's order -> 403 Forbidden")
    void preview_IdorViolation_ThrowsAccessDeniedException() {
        mockSecurityUser(otherCustomerId, "ROLE_CUSTOMER");

        MasterOrderJpaEntity masterOrder = new MasterOrderJpaEntity();
        masterOrder.setId(masterOrderId);
        masterOrder.setCustomerId(customerId);

        when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));

        assertThrows(AccessDeniedException.class, () -> orderController.getCancellationPreview(masterOrderId));
    }

    @Test
    @DisplayName("Order not found with given ID -> throws OrderNotFoundException")
    void preview_OrderNotFound_ThrowsOrderNotFoundException() {
        mockSecurityUser(customerId, "ROLE_CUSTOMER");

        when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.empty());
        when(subOrderRepository.findById(masterOrderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderController.getCancellationPreview(masterOrderId));
    }

    @Test
    @DisplayName("SubOrder missing slot -> preview handles null departure time with 0.0% refund")
    void preview_SubOrderMissingSlot_ReturnsZeroPercent() {
        mockSecurityUser(customerId, "ROLE_CUSTOMER");

        MasterOrderJpaEntity masterOrder = new MasterOrderJpaEntity();
        masterOrder.setId(masterOrderId);
        masterOrder.setCustomerId(customerId);

        SubOrderJpaEntity subOrder = new SubOrderJpaEntity();
        subOrder.setId(subOrderId);
        subOrder.setMasterOrderId(masterOrderId);
        subOrder.setSlotId(null);
        subOrder.setSubtotalAmount(new BigDecimal("500000.00"));

        when(masterOrderRepository.findById(masterOrderId)).thenReturn(Optional.of(masterOrder));
        when(subOrderRepository.findByMasterOrderId(masterOrderId)).thenReturn(List.of(subOrder));

        ResponseEntity<CancellationPreviewResponse> response = orderController.getCancellationPreview(masterOrderId);

        assertNotNull(response);
        CancellationPreviewResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(BigDecimal.valueOf(0.0), body.refundPercentage());
        assertEquals(0, BigDecimal.ZERO.compareTo(body.refundAmount()));
    }

    @Test
    @DisplayName("Customer creates order -> delegates to CreateOrderUseCase and returns 201 Created")
    void createOrder_CallsUseCase_Returns201Created() {
        mockSecurityUser(customerId, "ROLE_CUSTOMER");
        UUID bookingId = UUID.randomUUID();
        com.danasea.backend.modules.order.presentation.dtos.CreateOrderRequest request =
                new com.danasea.backend.modules.order.presentation.dtos.CreateOrderRequest(bookingId);
        String idempotencyKey = "order-create-idemp-12345";

        com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult detailResult =
                new com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult(
                        masterOrderId,
                        bookingId,
                        customerId,
                        com.danasea.backend.modules.order.domain.models.MasterOrderStatus.PENDING_PAYMENT,
                        com.danasea.backend.modules.order.domain.models.PaymentOrderStatus.UNPAID,
                        new BigDecimal("1000000"),
                        BigDecimal.ZERO,
                        null,
                        null,
                        idempotencyKey,
                        null,
                        List.of()
                );

        when(createOrderUseCase.execute(any(com.danasea.backend.modules.order.application.dtos.CreateOrderCommand.class)))
                .thenReturn(detailResult);

        ResponseEntity<com.danasea.backend.modules.order.presentation.dtos.OrderResponse> response =
                orderController.createOrder(request, idempotencyKey);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(masterOrderId, response.getBody().id());
        assertEquals(com.danasea.backend.modules.order.domain.models.MasterOrderStatus.PENDING_PAYMENT, response.getBody().status());
    }

    @Test
    @DisplayName("Customer gets order list -> delegates to GetCustomerOrdersUseCase and returns 200 OK")
    void getMyOrders_CallsUseCase_Returns200Ok() {
        mockSecurityUser(customerId, "ROLE_CUSTOMER");

        com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult item =
                new com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult(
                        masterOrderId,
                        UUID.randomUUID(),
                        customerId,
                        com.danasea.backend.modules.order.domain.models.MasterOrderStatus.PENDING_PAYMENT,
                        com.danasea.backend.modules.order.domain.models.PaymentOrderStatus.UNPAID,
                        new BigDecimal("500000"),
                        BigDecimal.ZERO,
                        null,
                        null,
                        "key",
                        null,
                        List.of()
                );

        com.danasea.backend.modules.order.domain.models.OrderPagedResult<com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult> paged =
                new com.danasea.backend.modules.order.domain.models.OrderPagedResult<>(
                        List.of(item), 0, 20, 1L, 1
                );

        when(getCustomerOrdersUseCase.execute(any(com.danasea.backend.modules.order.application.dtos.GetCustomerOrdersQuery.class)))
                .thenReturn(paged);

        ResponseEntity<com.danasea.backend.modules.order.presentation.dtos.OrderPageResponse<com.danasea.backend.modules.order.presentation.dtos.OrderResponse>> response =
                orderController.getMyOrders(0, 20);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().totalElements());
        assertEquals(masterOrderId, response.getBody().content().get(0).id());
    }

    @Test
    @DisplayName("User gets order detail -> delegates to GetOrderDetailUseCase and returns 200 OK")
    void getOrder_CallsUseCase_Returns200Ok() {
        mockSecurityUser(customerId, "ROLE_CUSTOMER");

        com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult detailResult =
                new com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult(
                        masterOrderId,
                        UUID.randomUUID(),
                        customerId,
                        com.danasea.backend.modules.order.domain.models.MasterOrderStatus.PAID,
                        com.danasea.backend.modules.order.domain.models.PaymentOrderStatus.PAID,
                        new BigDecimal("1000000"),
                        BigDecimal.ZERO,
                        null,
                        null,
                        "key",
                        null,
                        List.of()
                );

        when(getOrderDetailUseCase.execute(any(com.danasea.backend.modules.order.application.dtos.GetOrderDetailQuery.class)))
                .thenReturn(detailResult);

        ResponseEntity<com.danasea.backend.modules.order.presentation.dtos.OrderResponse> response =
                orderController.getOrder(masterOrderId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(masterOrderId, response.getBody().id());
        assertEquals(com.danasea.backend.modules.order.domain.models.MasterOrderStatus.PAID, response.getBody().status());
    }
}
