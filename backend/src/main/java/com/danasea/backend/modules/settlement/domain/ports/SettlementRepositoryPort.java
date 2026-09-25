package com.danasea.backend.modules.settlement.domain.ports;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.danasea.backend.modules.settlement.domain.models.Settlement;
import com.danasea.backend.modules.settlement.domain.models.SettlementStatus;

public interface SettlementRepositoryPort {

    Settlement save(Settlement settlement);

    Optional<Settlement> findById(UUID id);

    Optional<Settlement> findByIdAndVendorId(UUID id, UUID vendorId);

    Optional<Settlement> findByVendorIdAndPeriod(UUID vendorId, LocalDate periodStart, LocalDate periodEnd);

    boolean existsByVendorIdAndPeriodAndStatusIn(
            UUID vendorId, 
            LocalDate periodStart, 
            LocalDate periodEnd, 
            Collection<SettlementStatus> statuses);

    Page<Settlement> findAll(Pageable pageable);

    Page<Settlement> findByVendorId(UUID vendorId, Pageable pageable);

    void delete(Settlement settlement);

    void deleteLineItemsBySettlementId(UUID settlementId);
}
