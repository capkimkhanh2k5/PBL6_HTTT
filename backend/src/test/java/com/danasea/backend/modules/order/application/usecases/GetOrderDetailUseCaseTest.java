package com.danasea.backend.modules.order.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.order.application.dtos.GetOrderDetailQuery;
import com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.exceptions.UnauthorizedOrderAccessException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrderDetailUseCase Unit Tests")
class GetOrderDetailUseCaseTest {

    @Mock
    private MasterOrderRepositoryPort masterOrderRepository;
    @Mock
    private SubOrderRepositoryPort subOrderRepository;

    private GetOrderDetailUseCase useCase;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetOrderDetailUseCase(masterOrderRepository, subOrderRepository);
    }

    @Test
    @DisplayName("Thành công: Khách hàng xem đơn của chính mình")
    void shouldReturnOrderDetailWhenOwnerRequests() {
        MasterOrder order = MasterOrder.createFromBooking(
                ownerId, UUID.randomUUID(), new BigDecimal("350.00"), OffsetDateTime.now(), "key-1");
        order.setId(orderId);

        when(masterOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(subOrderRepository.findByMasterOrderId(orderId)).thenReturn(List.of());

        GetOrderDetailQuery query = new GetOrderDetailQuery(ownerId, orderId, false);
        MasterOrderDetailResult result = useCase.execute(query);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.customerId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("Thành công: Admin có quyền xem đơn hàng của bất kỳ ai")
    void shouldReturnOrderDetailWhenAdminRequests() {
        UUID adminId = UUID.randomUUID();
        MasterOrder order = MasterOrder.createFromBooking(
                ownerId, UUID.randomUUID(), new BigDecimal("350.00"), OffsetDateTime.now(), "key-1");
        order.setId(orderId);

        when(masterOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(subOrderRepository.findByMasterOrderId(orderId)).thenReturn(List.of());

        GetOrderDetailQuery query = new GetOrderDetailQuery(adminId, orderId, true);
        MasterOrderDetailResult result = useCase.execute(query);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Chặn IDOR (403): Khách hàng khác xem trộm đơn hàng")
    void shouldThrowUnauthorizedWhenDifferentCustomerRequests() {
        UUID strangerId = UUID.randomUUID();
        MasterOrder order = MasterOrder.createFromBooking(
                ownerId, UUID.randomUUID(), new BigDecimal("350.00"), OffsetDateTime.now(), "key-1");
        order.setId(orderId);

        when(masterOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        GetOrderDetailQuery query = new GetOrderDetailQuery(strangerId, orderId, false);

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("Đơn hàng không tồn tại (404): Ném OrderNotFoundException")
    void shouldThrowNotFoundWhenOrderDoesNotExist() {
        when(masterOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        GetOrderDetailQuery query = new GetOrderDetailQuery(ownerId, orderId, false);

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
