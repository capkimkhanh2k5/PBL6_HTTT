package com.danasea.backend.modules.order.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.application.dtos.GetOrderDetailQuery;
import com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.exceptions.UnauthorizedOrderAccessException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;

@Service
public class GetOrderDetailUseCase {

    private final MasterOrderRepositoryPort masterOrderRepository;
    private final SubOrderRepositoryPort subOrderRepository;

    public GetOrderDetailUseCase(
            MasterOrderRepositoryPort masterOrderRepository,
            SubOrderRepositoryPort subOrderRepository) {
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
    }

    @Transactional(readOnly = true)
    public MasterOrderDetailResult execute(GetOrderDetailQuery query) {
        if (query == null || query.orderId() == null || query.userId() == null) {
            throw new IllegalArgumentException("Order ID and User ID are required.");
        }

        MasterOrder order = masterOrderRepository.findById(query.orderId())
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));

        // Chặn IDOR: Khách hàng chỉ được xem đơn của chính mình, ngoại trừ Admin
        if (!query.isAdmin() && !query.userId().equals(order.getCustomerId())) {
            throw new UnauthorizedOrderAccessException(query.orderId(), query.userId());
        }

        order.setSubOrders(subOrderRepository.findByMasterOrderId(order.getId()));
        return MasterOrderDetailResult.fromDomain(order);
    }
}
