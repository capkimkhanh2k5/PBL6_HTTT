package com.danasea.backend.modules.dispute.infrastructure.persistence.adapters;

import com.danasea.backend.modules.dispute.domain.models.Dispute;
import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.dispute.domain.ports.DisputeRepositoryPort;
import com.danasea.backend.modules.dispute.infrastructure.persistence.entities.DisputeJpaEntity;
import com.danasea.backend.modules.dispute.infrastructure.persistence.mappers.DisputeMapper;
import com.danasea.backend.modules.dispute.infrastructure.persistence.repositories.JpaDisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DisputeRepositoryAdapter implements DisputeRepositoryPort {

    private final JpaDisputeRepository jpaDisputeRepository;
    private final DisputeMapper disputeMapper;

    private static final List<DisputeStatus> ACTIVE_STATUSES = List.of(DisputeStatus.OPEN, DisputeStatus.UNDER_REVIEW);

    @Override
    public Dispute save(Dispute dispute) {
        DisputeJpaEntity entity = disputeMapper.toEntity(dispute);
        DisputeJpaEntity saved = jpaDisputeRepository.save(entity);
        return disputeMapper.toDomain(saved);
    }

    @Override
    public Optional<Dispute> findById(UUID id) {
        return jpaDisputeRepository.findById(id).map(disputeMapper::toDomain);
    }

    @Override
    public boolean existsActiveDisputeForSubOrder(UUID subOrderId) {
        return jpaDisputeRepository.existsBySubOrderIdAndStatusIn(subOrderId, ACTIVE_STATUSES);
    }

    @Override
    public Optional<Dispute> findActiveDisputeBySubOrderId(UUID subOrderId) {
        return jpaDisputeRepository.findBySubOrderIdAndStatusIn(subOrderId, ACTIVE_STATUSES)
                .map(disputeMapper::toDomain);
    }

    @Override
    public List<Dispute> findBySubOrderId(UUID subOrderId) {
        return jpaDisputeRepository.findBySubOrderId(subOrderId).stream()
                .map(disputeMapper::toDomain)
                .toList();
    }

    @Override
    public Page<Dispute> findByCustomerId(UUID customerId, Pageable pageable) {
        return jpaDisputeRepository.findByCustomerId(customerId, pageable)
                .map(disputeMapper::toDomain);
    }

    @Override
    public Page<Dispute> findWithFilters(DisputeStatus status, DisputeReason reason, Pageable pageable) {
        return jpaDisputeRepository.findFiltered(status, reason, pageable)
                .map(disputeMapper::toDomain);
    }

    @Override
    public List<Dispute> findActiveDisputesBySubOrderIds(Collection<UUID> subOrderIds) {
        return jpaDisputeRepository.findBySubOrderIdInAndStatusIn(subOrderIds, ACTIVE_STATUSES).stream()
                .map(disputeMapper::toDomain)
                .toList();
    }
}
