package com.danasea.backend.modules.order.domain.ports;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain Port tra cứu thời điểm khởi hành của ServiceSlot để phục vụ tính toán hoàn tiền và hủy dịch vụ.
 */
public interface ServiceSlotDepartureLookupPort {

    Optional<LocalDateTime> findDepartureTime(UUID slotId);
}
