package com.danasea.backend.modules.service.domain.ports;

import java.util.UUID;

public interface AuditLogPort {
    void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);
}
