package com.danasea.backend.modules.settlement.infrastructure.adapters;

import com.danasea.backend.modules.settlement.domain.models.Settlement;
import com.danasea.backend.modules.settlement.domain.models.SettlementLineItem;
import com.danasea.backend.modules.settlement.domain.models.SettlementStatus;
import com.danasea.backend.modules.settlement.domain.ports.SettlementRepositoryPort;
import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementJpaEntity;
import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementLineItemJpaEntity;
import com.danasea.backend.modules.settlement.infrastructure.persistence.mappers.SettlementMapper;
import com.danasea.backend.modules.settlement.infrastructure.persistence.repositories.JpaSettlementLineItemRepository;
import com.danasea.backend.modules.settlement.infrastructure.persistence.repositories.JpaSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SettlementRepositoryAdapter implements SettlementRepositoryPort {

    private final JpaSettlementRepository jpaSettlementRepository;
    private final JpaSettlementLineItemRepository jpaSettlementLineItemRepository;
    private final SettlementMapper settlementMapper;

    @Override
    @Transactional
    public Settlement save(Settlement settlement) {
        SettlementJpaEntity entity = settlementMapper.toEntity(settlement);
        SettlementJpaEntity savedEntity = jpaSettlementRepository.save(entity);

        if (settlement.getLineItems() != null && !settlement.getLineItems().isEmpty()) {
            for (SettlementLineItem item : settlement.getLineItems()) {
                item.setSettlementId(savedEntity.getId());
                SettlementLineItemJpaEntity itemEntity = settlementMapper.toLineItemEntity(item);
                jpaSettlementLineItemRepository.save(itemEntity);
            }
        }

        List<SettlementLineItemJpaEntity> lineItems = jpaSettlementLineItemRepository.findBySettlementId(savedEntity.getId());
        return settlementMapper.toDomain(savedEntity, lineItems);
    }

    @Override
    public Optional<Settlement> findById(UUID id) {
        return jpaSettlementRepository.findById(id)
                .map(entity -> {
                    List<SettlementLineItemJpaEntity> items = jpaSettlementLineItemRepository.findBySettlementId(entity.getId());
                    return settlementMapper.toDomain(entity, items);
                });
    }

    @Override
    public Optional<Settlement> findByIdAndVendorId(UUID id, UUID vendorId) {
        return jpaSettlementRepository.findById(id)
                .filter(entity -> entity.getVendorId().equals(vendorId))
                .map(entity -> {
                    List<SettlementLineItemJpaEntity> items = jpaSettlementLineItemRepository.findBySettlementId(entity.getId());
                    return settlementMapper.toDomain(entity, items);
                });
    }

    @Override
    public Optional<Settlement> findByVendorIdAndPeriod(UUID vendorId, LocalDate periodStart, LocalDate periodEnd) {
        return jpaSettlementRepository.findByVendorIdAndPeriodStartAndPeriodEnd(vendorId, periodStart, periodEnd)
                .map(entity -> {
                    List<SettlementLineItemJpaEntity> items = jpaSettlementLineItemRepository.findBySettlementId(entity.getId());
                    return settlementMapper.toDomain(entity, items);
                });
    }

    @Override
    public boolean existsByVendorIdAndPeriodAndStatusIn(
            UUID vendorId,
            LocalDate periodStart,
            LocalDate periodEnd,
            Collection<SettlementStatus> statuses
    ) {
        return jpaSettlementRepository.findByVendorIdAndPeriodStartAndPeriodEnd(vendorId, periodStart, periodEnd)
                .filter(s -> statuses.contains(s.getStatus()))
                .isPresent();
    }

    @Override
    public Page<Settlement> findAll(Pageable pageable) {
        return jpaSettlementRepository.findAll(pageable)
                .map(entity -> settlementMapper.toDomain(entity, null));
    }

    @Override
    public Page<Settlement> findByVendorId(UUID vendorId, Pageable pageable) {
        return jpaSettlementRepository.findByVendorId(vendorId, pageable)
                .map(entity -> settlementMapper.toDomain(entity, null));
    }

    @Override
    @Transactional
    public void delete(Settlement settlement) {
        if (settlement.getId() != null) {
            jpaSettlementLineItemRepository.deleteBySettlementId(settlement.getId());
            jpaSettlementRepository.deleteById(settlement.getId());
        }
    }

    @Override
    @Transactional
    public void deleteLineItemsBySettlementId(UUID settlementId) {
        jpaSettlementLineItemRepository.deleteBySettlementId(settlementId);
    }
}
