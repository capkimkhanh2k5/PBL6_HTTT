package com.danasea.backend.modules.order.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.application.dtos.GetCustomerOrdersQuery;
import com.danasea.backend.modules.order.application.dtos.MasterOrderDetailResult;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.OrderPagedResult;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;

@Service
public class GetCustomerOrdersUseCase {

    private final MasterOrderRepositoryPort masterOrderRepository;
    private final SubOrderRepositoryPort subOrderRepository;

    public GetCustomerOrdersUseCase(
            MasterOrderRepositoryPort masterOrderRepository,
            SubOrderRepositoryPort subOrderRepository) {
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
    }

    @Transactional(readOnly = true)
    public OrderPagedResult<MasterOrderDetailResult> execute(GetCustomerOrdersQuery query) {
        if (query == null || query.customerId() == null) {
            throw new IllegalArgumentException("Customer ID is required.");
        }
        int page = Math.max(0, query.page());
        int size = query.size() <= 0 ? 10 : Math.min(query.size(), 100);

        OrderPagedResult<MasterOrder> paged = masterOrderRepository.findByCustomerId(query.customerId(), page, size);

        List<MasterOrderDetailResult> results = paged.content().stream().map(order -> {
            order.setSubOrders(subOrderRepository.findByMasterOrderId(order.getId()));
            return MasterOrderDetailResult.fromDomain(order);
        }).toList();

        return new OrderPagedResult<>(
                results,
                paged.page(),
                paged.size(),
                paged.totalElements(),
                paged.totalPages()
        );
    }
}
