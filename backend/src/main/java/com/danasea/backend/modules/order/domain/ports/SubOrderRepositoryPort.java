package com.danasea.backend.modules.order.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.OrderPagedResult;
import com.danasea.backend.modules.order.domain.models.SubOrder;

public interface SubOrderRepositoryPort {

    SubOrder save(SubOrder subOrder);

    List<SubOrder> saveAll(List<SubOrder> subOrders);

    Optional<SubOrder> findById(UUID id);

    Optional<SubOrder> findByBookingItemId(UUID bookingItemId);

    Optional<SubOrder> findByBookingItemIdForUpdate(UUID bookingItemId);

    List<SubOrder> findByMasterOrderId(UUID masterOrderId);

    OrderPagedResult<SubOrder> findByVendorId(UUID vendorId, int page, int size);
}
