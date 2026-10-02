package com.danasea.backend.modules.order.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.application.dtos.GetVendorOrdersQuery;
import com.danasea.backend.modules.order.application.dtos.SubOrderDetailResult;
import com.danasea.backend.modules.order.domain.models.OrderPagedResult;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;

@Service
public class GetVendorOrdersUseCase {

    private final SubOrderRepositoryPort subOrderRepository;

    public GetVendorOrdersUseCase(SubOrderRepositoryPort subOrderRepository) {
        this.subOrderRepository = subOrderRepository;
    }

    @Transactional(readOnly = true)
    public OrderPagedResult<SubOrderDetailResult> execute(GetVendorOrdersQuery query) {
        if (query == null || query.vendorId() == null) {
            throw new IllegalArgumentException("Vendor ID is required.");
        }
        int page = Math.max(0, query.page());
        int size = query.size() <= 0 ? 10 : Math.min(query.size(), 100);

        OrderPagedResult<SubOrder> paged = subOrderRepository.findByVendorId(query.vendorId(), page, size);

        List<SubOrderDetailResult> results = paged.content().stream()
                .map(SubOrderDetailResult::fromDomain)
                .toList();

        return new OrderPagedResult<>(
                results,
                paged.page(),
                paged.size(),
                paged.totalElements(),
                paged.totalPages()
        );
    }
}
