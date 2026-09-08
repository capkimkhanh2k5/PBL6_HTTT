package com.danasea.backend.modules.account.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.account.infrastructure.persistence.entities.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {
}
