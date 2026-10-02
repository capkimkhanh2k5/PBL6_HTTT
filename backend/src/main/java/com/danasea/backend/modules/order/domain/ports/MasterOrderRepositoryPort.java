package com.danasea.backend.modules.order.domain.ports;

import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.OrderPagedResult;

public interface MasterOrderRepositoryPort {

    MasterOrder save(MasterOrder order);

    Optional<MasterOrder> findById(UUID id);

    Optional<MasterOrder> findByIdForUpdate(UUID id);

    Optional<MasterOrder> findByBookingId(UUID bookingId);

    Optional<MasterOrder> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    OrderPagedResult<MasterOrder> findByCustomerId(UUID customerId, int page, int size);
}
