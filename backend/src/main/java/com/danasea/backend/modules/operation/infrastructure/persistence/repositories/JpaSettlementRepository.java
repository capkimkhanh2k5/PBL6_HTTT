package com.danasea.backend.modules.operation.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.operation.infrastructure.persistence.entities.SettlementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaSettlementRepository extends JpaRepository<SettlementJpaEntity, UUID> {
}
