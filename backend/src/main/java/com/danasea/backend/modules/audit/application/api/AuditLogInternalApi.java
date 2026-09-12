package com.danasea.backend.modules.audit.application.api;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.danasea.backend.modules.audit.domain.models.AuditLog;

public interface AuditLogInternalApi {
    void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);

    Page<AuditLog> getAuditLogs(Pageable pageable);

    Optional<AuditLog> getAuditLogById(UUID id);
}
