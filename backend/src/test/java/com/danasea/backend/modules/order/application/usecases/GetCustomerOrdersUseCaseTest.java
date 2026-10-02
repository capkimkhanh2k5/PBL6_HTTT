package com.danasea.backend.modules.order.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.order.application.dtos.GetCustomerOrdersQuery;
import com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.OrderPagedResult;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetCustomerOrdersUseCase Unit Tests")
class GetCustomerOrdersUseCaseTest {

    @Mock
    private MasterOrderRepositoryPort masterOrderRepository;

    @Mock
    private SubOrderRepositoryPort subOrderRepository;

    private GetCustomerOrdersUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId1 = UUID.randomUUID();
    private final UUID orderId2 = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetCustomerOrdersUseCase(masterOrderRepository, subOrderRepository);
    }

    @Test
    @DisplayName("Thành công: Lấy danh sách đơn hàng có phân trang hợp lệ và map SubOrders")
    void shouldReturnPagedOrdersWithSubOrdersSuccessfully() {
        MasterOrder order1 = MasterOrder.createFromBooking(
                customerId, bookingId, new BigDecimal("1200000.00"), OffsetDateTime.now(), "key-order-1");
        order1.setId(orderId1);
        order1.setStatus(MasterOrderStatus.PAID);
        order1.setPaymentStatus(PaymentOrderStatus.PAID);

        SubOrder subOrder1 = new SubOrder();
        subOrder1.setId(UUID.randomUUID());
        subOrder1.setMasterOrderId(orderId1);
        subOrder1.setVendorId(UUID.randomUUID());
        subOrder1.setServiceId(UUID.randomUUID());
        subOrder1.setSlotId(UUID.randomUUID());
        subOrder1.setQuantity(2);
        subOrder1.setUnitPrice(new BigDecimal("600000.00"));
        subOrder1.setSubtotalAmount(new BigDecimal("1200000.00"));
        subOrder1.setStatus(SubOrderStatus.CONFIRMED);

        OrderPagedResult<MasterOrder> repoPaged = new OrderPagedResult<>(
                List.of(order1), 0, 10, 1L, 1);

        when(masterOrderRepository.findByCustomerId(customerId, 0, 10)).thenReturn(repoPaged);
        when(subOrderRepository.findByMasterOrderId(orderId1)).thenReturn(List.of(subOrder1));

        GetCustomerOrdersQuery query = new GetCustomerOrdersQuery(customerId, 0, 10);
        OrderPagedResult<MasterOrderDetailResult> result = useCase.execute(query);

        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.content()).hasSize(1);

        MasterOrderDetailResult detail = result.content().get(0);
        assertThat(detail.id()).isEqualTo(orderId1);
        assertThat(detail.customerId()).isEqualTo(customerId);
        assertThat(detail.bookingId()).isEqualTo(bookingId);
        assertThat(detail.status()).isEqualTo(MasterOrderStatus.PAID);
        assertThat(detail.paymentStatus()).isEqualTo(PaymentOrderStatus.PAID);
        assertThat(detail.totalAmount()).isEqualByComparingTo("1200000.00");
        assertThat(detail.subOrders()).hasSize(1);

        var subDetail = detail.subOrders().get(0);
        assertThat(subDetail.id()).isEqualTo(subOrder1.getId());
        assertThat(subDetail.quantity()).isEqualTo(2);
        assertThat(subDetail.status()).isEqualTo(SubOrderStatus.CONFIRMED);
        assertThat(subDetail.subtotalAmount()).isEqualByComparingTo("1200000.00");

        verify(masterOrderRepository).findByCustomerId(customerId, 0, 10);
        verify(subOrderRepository).findByMasterOrderId(orderId1);
    }

    @Test
    @DisplayName("Kiểm tra ranh giới userId/IDOR: Bắt buộc customerId, không được null")
    void shouldThrowWhenQueryOrCustomerIdIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer ID is required.");

        GetCustomerOrdersQuery nullCustomerQuery = new GetCustomerOrdersQuery(null, 0, 10);
        assertThatThrownBy(() -> useCase.execute(nullCustomerQuery))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer ID is required.");

        verify(masterOrderRepository, never()).findByCustomerId(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Chuẩn hóa phân trang: page âm -> 0, size <= 0 -> 10, size > 100 -> 100")
    void shouldNormalizePaginationBoundaries() {
        OrderPagedResult<MasterOrder> emptyResult = new OrderPagedResult<>(List.of(), 0, 10, 0L, 0);

        // Case 1: page < 0, size <= 0 -> normalized to page 0, size 10
        when(masterOrderRepository.findByCustomerId(customerId, 0, 10)).thenReturn(emptyResult);
        useCase.execute(new GetCustomerOrdersQuery(customerId, -5, -1));
        verify(masterOrderRepository).findByCustomerId(customerId, 0, 10);

        // Case 2: size > 100 -> normalized to size 100
        OrderPagedResult<MasterOrder> maxResult = new OrderPagedResult<>(List.of(), 2, 100, 0L, 0);
        when(masterOrderRepository.findByCustomerId(customerId, 2, 100)).thenReturn(maxResult);
        useCase.execute(new GetCustomerOrdersQuery(customerId, 2, 500));
        verify(masterOrderRepository).findByCustomerId(customerId, 2, 100);
    }

    @Test
    @DisplayName("Ranh giới IDOR: Đảm bảo chỉ tra cứu đơn của chính customerId được yêu cầu")
    void shouldStrictlyQueryByRequestedCustomerId() {
        UUID otherCustomerId = UUID.randomUUID();
        OrderPagedResult<MasterOrder> emptyResult = new OrderPagedResult<>(List.of(), 0, 10, 0L, 0);
        when(masterOrderRepository.findByCustomerId(otherCustomerId, 0, 10)).thenReturn(emptyResult);

        OrderPagedResult<MasterOrderDetailResult> result =
                useCase.execute(new GetCustomerOrdersQuery(otherCustomerId, 0, 10));

        assertThat(result.content()).isEmpty();
        verify(masterOrderRepository).findByCustomerId(eq(otherCustomerId), eq(0), eq(10));
        verify(masterOrderRepository, never()).findByCustomerId(eq(customerId), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Khách hàng chưa có đơn hàng nào -> Trả về danh sách rỗng an toàn")
    void shouldReturnEmptyResultWhenCustomerHasNoOrders() {
        OrderPagedResult<MasterOrder> emptyPaged = new OrderPagedResult<>(List.of(), 0, 20, 0L, 0);
        when(masterOrderRepository.findByCustomerId(customerId, 0, 20)).thenReturn(emptyPaged);

        OrderPagedResult<MasterOrderDetailResult> result =
                useCase.execute(new GetCustomerOrdersQuery(customerId, 0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0L);
        assertThat(result.totalPages()).isEqualTo(0);
        verify(subOrderRepository, never()).findByMasterOrderId(any());
    }

    @Test
    @DisplayName("Mapping nhiều đơn hàng với nhiều SubOrders ở các trạng thái khác nhau")
    void shouldMapMultipleOrdersWithMultipleSubOrders() {
        MasterOrder order1 = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("500000.00"), OffsetDateTime.now(), "k1");
        order1.setId(orderId1);
        order1.setStatus(MasterOrderStatus.PENDING_PAYMENT);

        MasterOrder order2 = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("800000.00"), OffsetDateTime.now(), "k2");
        order2.setId(orderId2);
        order2.setStatus(MasterOrderStatus.PARTIALLY_COMPLETED);

        SubOrder sub1 = new SubOrder();
        sub1.setId(UUID.randomUUID());
        sub1.setMasterOrderId(orderId1);
        sub1.setStatus(SubOrderStatus.PENDING);

        SubOrder sub2a = new SubOrder();
        sub2a.setId(UUID.randomUUID());
        sub2a.setMasterOrderId(orderId2);
        sub2a.setStatus(SubOrderStatus.COMPLETED);

        SubOrder sub2b = new SubOrder();
        sub2b.setId(UUID.randomUUID());
        sub2b.setMasterOrderId(orderId2);
        sub2b.setStatus(SubOrderStatus.CANCELLED);

        OrderPagedResult<MasterOrder> paged = new OrderPagedResult<>(
                List.of(order1, order2), 0, 10, 2L, 1);

        when(masterOrderRepository.findByCustomerId(customerId, 0, 10)).thenReturn(paged);
        when(subOrderRepository.findByMasterOrderId(orderId1)).thenReturn(List.of(sub1));
        when(subOrderRepository.findByMasterOrderId(orderId2)).thenReturn(List.of(sub2a, sub2b));

        OrderPagedResult<MasterOrderDetailResult> result =
                useCase.execute(new GetCustomerOrdersQuery(customerId, 0, 10));

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).subOrders()).hasSize(1);
        assertThat(result.content().get(0).subOrders().get(0).status()).isEqualTo(SubOrderStatus.PENDING);
        assertThat(result.content().get(1).subOrders()).hasSize(2);
        assertThat(result.content().get(1).subOrders().get(0).status()).isEqualTo(SubOrderStatus.COMPLETED);
        assertThat(result.content().get(1).subOrders().get(1).status()).isEqualTo(SubOrderStatus.CANCELLED);
    }
}
