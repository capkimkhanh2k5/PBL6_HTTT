package com.danasea.backend.modules.dispute.domain.ports;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.danasea.backend.modules.dispute.domain.models.Dispute;
import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;

public interface DisputeRepositoryPort {

    Dispute save(Dispute dispute);

    Optional<Dispute> findById(UUID id);

    boolean existsActiveDisputeForSubOrder(UUID subOrderId);

    Optional<Dispute> findActiveDisputeBySubOrderId(UUID subOrderId);

    List<Dispute> findBySubOrderId(UUID subOrderId);

    Page<Dispute> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Dispute> findWithFilters(DisputeStatus status, DisputeReason reason, Pageable pageable);

    List<Dispute> findActiveDisputesBySubOrderIds(Collection<UUID> subOrderIds);
}
