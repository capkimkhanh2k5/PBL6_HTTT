package com.danasea.backend.modules.order.presentation.controllers;

import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.application.usecases.CreateOrderUseCase;
import com.danasea.backend.modules.order.application.usecases.GetCancellationPreviewUseCase;
import com.danasea.backend.modules.order.application.usecases.GetCustomerOrdersUseCase;
import com.danasea.backend.modules.order.application.usecases.GetOrderDetailUseCase;
import com.danasea.backend.modules.order.application.usecases.RequestRefundUseCase;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.ports.*;
import com.danasea.backend.modules.order.presentation.handlers.OrderExceptionHandler;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.presentation.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Ownership & Idempotency HTTP Regression Tests (Account A/B)")
class OrderOwnershipHttpRegressionTest {

    @Mock
    private MasterOrderRepositoryPort masterOrderRepository;

    @Mock
    private SubOrderRepositoryPort subOrderRepository;

    @Mock
    private BookingLookupPort bookingLookupPort;

    @Mock
    private BookingStatusUpdatePort bookingStatusUpdatePort;

    @Mock
    private CommissionPolicyPort commissionPolicyPort;

    @Mock
    private OrderEventPublisherPort orderEventPublisherPort;

    @Mock
    private GetOrderDetailUseCase getOrderDetailUseCase;

    @Mock
    private GetCustomerOrdersUseCase getCustomerOrdersUseCase;

    @Mock
    private RequestRefundUseCase requestRefundUseCase;

    @Mock
    private GetCancellationPreviewUseCase getCancellationPreviewUseCase;

    @Mock
    private OrderPaymentService orderPaymentService;

    private MockMvc mockMvc;

    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();
    private final UUID bookingIdA = UUID.randomUUID();
    private final UUID bookingIdB = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        CreateOrderUseCase createOrderUseCase = new CreateOrderUseCase(
                masterOrderRepository,
                subOrderRepository,
                bookingLookupPort,
                bookingStatusUpdatePort,
                commissionPolicyPort,
                orderEventPublisherPort
        );

        OrderController orderController = new OrderController(
                createOrderUseCase,
                getOrderDetailUseCase,
                getCustomerOrdersUseCase,
                requestRefundUseCase,
                getCancellationPreviewUseCase,
                orderPaymentService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(
                        new OrderExceptionHandler(),
                        new GlobalExceptionHandler(LocalizedMessageService.standalone())
                )
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("POST /api/orders: Account A tạo đơn mới thành công từ booking của mình -> 201 Created")
    void createOrder_OwnerA_FirstTime_Returns201() throws Exception {
        authenticateAs(userA);

        when(masterOrderRepository.findByBookingId(bookingIdA)).thenReturn(Optional.empty());
        when(masterOrderRepository.findByCustomerIdAndIdempotencyKey(userA, "idem-A-1")).thenReturn(Optional.empty());

        BookingOrderView.BookingItemOrderView item = new BookingOrderView.BookingItemOrderView(
                UUID.randomUUID(), vendorId, UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("500.00"));
        BookingOrderView bookingView = new BookingOrderView(
                bookingIdA, userA, "HOLD", new BigDecimal("500.00"), OffsetDateTime.now().plusMinutes(15), List.of(item));

        when(bookingLookupPort.findBookingForOrderForUpdate(bookingIdA)).thenReturn(Optional.of(bookingView));
        when(commissionPolicyPort.getCommissionRate(vendorId)).thenReturn(new BigDecimal("0.10"));

        UUID generatedOrderId = UUID.randomUUID();
        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(invocation -> {
            MasterOrder order = invocation.getArgument(0);
            order.setId(generatedOrderId);
            return order;
        });
        when(subOrderRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String body = """
                {
                    "bookingId": "%s"
                }
                """.formatted(bookingIdA);

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "idem-A-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(generatedOrderId.toString()))
                .andExpect(jsonPath("$.bookingId").value(bookingIdA.toString()))
                .andExpect(jsonPath("$.customerId").value(userA.toString()))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        verify(bookingStatusUpdatePort).updateStatusToPendingPayment(bookingIdA);
    }

    @Test
    @DisplayName("POST /api/orders: Account A gửi lại request lặp (Idempotent replay) với cùng bookingId -> 201 Created trả về Order cũ")
    void createOrder_OwnerA_IdempotentReplay_ReturnsExistingOrder() throws Exception {
        authenticateAs(userA);

        UUID existingOrderId = UUID.randomUUID();
        MasterOrder existingOrder = MasterOrder.createFromBooking(
                userA, bookingIdA, new BigDecimal("500.00"), OffsetDateTime.now().plusMinutes(10), "idem-A-1");
        existingOrder.setId(existingOrderId);

        when(masterOrderRepository.findByBookingId(bookingIdA)).thenReturn(Optional.of(existingOrder));
        when(subOrderRepository.findByMasterOrderId(existingOrderId)).thenReturn(List.of());

        String body = """
                {
                    "bookingId": "%s"
                }
                """.formatted(bookingIdA);

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "idem-A-retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(existingOrderId.toString()))
                .andExpect(jsonPath("$.bookingId").value(bookingIdA.toString()))
                .andExpect(jsonPath("$.customerId").value(userA.toString()));

        verify(bookingLookupPort, never()).findBookingForOrderForUpdate(any());
        verify(masterOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /api/orders: Account B gửi request lặp với bookingId của Account A (order đã tồn tại) -> 403 Forbidden UNAUTHORIZED_ORDER_ACCESS")
    void createOrder_AttackerB_IdempotentReplayWithOtherUserBooking_Returns403() throws Exception {
        authenticateAs(userB);

        UUID existingOrderId = UUID.randomUUID();
        // Đơn hàng cũ thuộc sở hữu của userA
        MasterOrder existingOrder = MasterOrder.createFromBooking(
                userA, bookingIdA, new BigDecimal("500.00"), OffsetDateTime.now().plusMinutes(10), "idem-A-1");
        existingOrder.setId(existingOrderId);

        // Nhánh 1: findByBookingId tìm thấy đơn cũ của User A
        when(masterOrderRepository.findByBookingId(bookingIdA)).thenReturn(Optional.of(existingOrder));

        String body = """
                {
                    "bookingId": "%s"
                }
                """.formatted(bookingIdA);

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "idem-B-probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_ORDER_ACCESS"));

        // Dữ liệu suborders của User A tuyệt đối không được đọc trả về cho User B
        verify(subOrderRepository, never()).findByMasterOrderId(any());
        verify(masterOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /api/orders: Account B gửi request với idempotencyKey trỏ tới order của Account A -> 403 Forbidden UNAUTHORIZED_ORDER_ACCESS")
    void createOrder_AttackerB_IdempotentKeyBelongsToOtherUser_Returns403() throws Exception {
        authenticateAs(userB);

        UUID existingOrderId = UUID.randomUUID();
        MasterOrder existingOrder = MasterOrder.createFromBooking(
                userA, bookingIdA, new BigDecimal("500.00"), OffsetDateTime.now().plusMinutes(10), "shared-idem-key");
        existingOrder.setId(existingOrderId);

        when(masterOrderRepository.findByBookingId(bookingIdB)).thenReturn(Optional.empty());
        when(masterOrderRepository.findByCustomerIdAndIdempotencyKey(userB, "shared-idem-key"))
                .thenReturn(Optional.of(existingOrder));

        String body = """
                {
                    "bookingId": "%s"
                }
                """.formatted(bookingIdB);

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "shared-idem-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_ORDER_ACCESS"));

        verify(subOrderRepository, never()).findByMasterOrderId(any());
    }

    @Test
    @DisplayName("POST /api/orders: Account B cố tình tạo đơn mới từ booking của Account A khi chưa có order -> 403 Forbidden UNAUTHORIZED_ORDER_ACCESS")
    void createOrder_AttackerB_NewOrderFromOtherUserBooking_Returns403() throws Exception {
        authenticateAs(userB);

        when(masterOrderRepository.findByBookingId(bookingIdA)).thenReturn(Optional.empty());
        when(masterOrderRepository.findByCustomerIdAndIdempotencyKey(userB, "idem-B-new")).thenReturn(Optional.empty());

        BookingOrderView.BookingItemOrderView item = new BookingOrderView.BookingItemOrderView(
                UUID.randomUUID(), vendorId, UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("500.00"));
        // Booking thuộc về userA
        BookingOrderView bookingViewOfA = new BookingOrderView(
                bookingIdA, userA, "HOLD", new BigDecimal("500.00"), OffsetDateTime.now().plusMinutes(15), List.of(item));

        when(bookingLookupPort.findBookingForOrderForUpdate(bookingIdA)).thenReturn(Optional.of(bookingViewOfA));

        String body = """
                {
                    "bookingId": "%s"
                }
                """.formatted(bookingIdA);

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "idem-B-new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_ORDER_ACCESS"));

        verify(masterOrderRepository, never()).save(any());
    }
}
