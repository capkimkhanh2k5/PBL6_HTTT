package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.service.domain.ports.AuditLogPort;
import lombok.RequiredArgsConstructor;

@Component("serviceAuditLogAdapter")
@RequiredArgsConstructor
public class ServiceAuditLogAdapter implements AuditLogPort {

    private final AccountInternalApi accountInternalApi;

    @Override
    public void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata) {
        accountInternalApi.recordAuditLog(actorUserId, action, entityType, entityId, metadata);
    }
}
