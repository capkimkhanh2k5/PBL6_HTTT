package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.modules.service.domain.ports.AuditLogPort;
import lombok.RequiredArgsConstructor;

@Component("serviceAuditLogAdapter")
@RequiredArgsConstructor
public class ServiceAuditLogAdapter implements AuditLogPort {

    private final AuditLogInternalApi auditLogInternalApi;

    @Override
    public void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata) {
        auditLogInternalApi.recordAuditLog(actorUserId, action, entityType, entityId, metadata);
    }
}
