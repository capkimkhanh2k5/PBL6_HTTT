package com.danasea.backend.modules.order.domain.ports;

import com.danasea.backend.modules.order.domain.models.OrderPagedResult;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubOrderRepositoryPort {

    SubOrder save(SubOrder subOrder);

    List<SubOrder> saveAll(List<SubOrder> subOrders);

    Optional<SubOrder> findById(UUID id);

    default Optional<UUID> findMasterOrderIdById(UUID id) {
        return findById(id).map(SubOrder::getMasterOrderId);
    }

    Optional<SubOrder> findByIdForUpdate(UUID id);

    Optional<SubOrder> findByBookingItemId(UUID bookingItemId);

    Optional<SubOrder> findByBookingItemIdForUpdate(UUID bookingItemId);

    List<SubOrder> findByMasterOrderId(UUID masterOrderId);

    List<SubOrder> findByMasterOrderIdForUpdate(UUID masterOrderId);

    OrderPagedResult<SubOrder> findByVendorId(UUID vendorId, int page, int size);
}
