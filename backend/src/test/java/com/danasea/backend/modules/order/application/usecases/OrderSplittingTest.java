package com.danasea.backend.modules.order.application.usecases;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.order.application.dtos.CreateOrderCommand;
import com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.ports.BookingLookupPort;
import com.danasea.backend.modules.order.domain.ports.BookingOrderView;
import com.danasea.backend.modules.order.domain.ports.BookingStatusUpdatePort;
import com.danasea.backend.modules.order.domain.ports.CommissionPolicyPort;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.OrderEventPublisherPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSplittingTest - Sub-order 1:1 Splitting & Multi-Vendor Verification")
class OrderSplittingTest {

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

    private CreateOrderUseCase createOrderUseCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID vendorA = UUID.randomUUID();
    private final UUID vendorB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createOrderUseCase = new CreateOrderUseCase(
                masterOrderRepository,
                subOrderRepository,
                bookingLookupPort,
                bookingStatusUpdatePort,
                commissionPolicyPort,
                orderEventPublisherPort
        );
    }

    @Test
    @DisplayName("Splits Booking items across multiple vendors: 1 SubOrder = 1 BookingItem with individual vendor commission")
    void shouldSplitCorrectlyPerVendorAndItem() {
        // Vendor A has 2 booking items, Vendor B has 1 booking item
        UUID itemA1 = UUID.randomUUID();
        UUID itemA2 = UUID.randomUUID();
        UUID itemB1 = UUID.randomUUID();

        BookingOrderView.BookingItemOrderView viewA1 = new BookingOrderView.BookingItemOrderView(
                itemA1, vendorA, UUID.randomUUID(), UUID.randomUUID(), 2, new BigDecimal("200000")); // total 400,000
        BookingOrderView.BookingItemOrderView viewA2 = new BookingOrderView.BookingItemOrderView(
                itemA2, vendorA, UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("300000")); // total 300,000
        BookingOrderView.BookingItemOrderView viewB1 = new BookingOrderView.BookingItemOrderView(
                itemB1, vendorB, UUID.randomUUID(), UUID.randomUUID(), 3, new BigDecimal("100000")); // total 300,000

        BookingOrderView bookingView = new BookingOrderView(
                bookingId,
                customerId,
                "HOLD",
                new BigDecimal("1000000"),
                OffsetDateTime.now().plusMinutes(15),
                List.of(viewA1, viewA2, viewB1)
        );

        when(masterOrderRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(masterOrderRepository.findByCustomerIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(bookingLookupPort.findBookingForOrder(bookingId)).thenReturn(Optional.of(bookingView));

        // Vendor A: 10% commission, Vendor B: 15% commission
        when(commissionPolicyPort.getCommissionRate(vendorA)).thenReturn(new BigDecimal("0.10"));
        when(commissionPolicyPort.getCommissionRate(vendorB)).thenReturn(new BigDecimal("0.15"));

        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(inv -> {
            MasterOrder o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SubOrder>> subOrdersCaptor = ArgumentCaptor.forClass(List.class);
        when(subOrderRepository.saveAll(subOrdersCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        MasterOrderDetailResult result = createOrderUseCase.execute(
                new CreateOrderCommand(customerId, bookingId, "order-split-idem-12345"));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.subOrders()).hasSize(3);

        List<SubOrder> savedSubOrders = subOrdersCaptor.getValue();
        assertThat(savedSubOrders).hasSize(3);

        // Verify Vendor A SubOrders
        List<SubOrder> subOrdersA = savedSubOrders.stream().filter(s -> s.getVendorId().equals(vendorA)).toList();
        assertThat(subOrdersA).hasSize(2);
        assertThat(subOrdersA.get(0).getCommissionRate()).isEqualByComparingTo("0.10");
        assertThat(subOrdersA.get(0).getStatus()).isEqualTo(SubOrderStatus.PENDING);

        // Verify Vendor B SubOrder
        List<SubOrder> subOrdersB = savedSubOrders.stream().filter(s -> s.getVendorId().equals(vendorB)).toList();
        assertThat(subOrdersB).hasSize(1);
        assertThat(subOrdersB.get(0).getCommissionRate()).isEqualByComparingTo("0.15");
        assertThat(subOrdersB.get(0).getSubtotalAmount()).isEqualByComparingTo("300000");
        assertThat(subOrdersB.get(0).getCommissionAmount()).isEqualByComparingTo("45000"); // 300,000 * 15%
        assertThat(subOrdersB.get(0).getVendorPayoutAmount()).isEqualByComparingTo("255000");

        // Verify order creation event published
        verify(orderEventPublisherPort).publishOrderCreatedEvent(any(MasterOrder.class));
    }
}
