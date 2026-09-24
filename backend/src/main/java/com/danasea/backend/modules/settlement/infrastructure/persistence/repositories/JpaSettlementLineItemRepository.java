package com.danasea.backend.modules.settlement.infrastructure.persistence.repositories;

import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementLineItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository("settlementJpaSettlementLineItemRepository")
public interface JpaSettlementLineItemRepository extends JpaRepository<SettlementLineItemJpaEntity, UUID> {

    List<SettlementLineItemJpaEntity> findBySettlementId(UUID settlementId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SettlementLineItemJpaEntity e WHERE e.settlementId = :settlementId")
    void deleteBySettlementId(@Param("settlementId") UUID settlementId);
}
