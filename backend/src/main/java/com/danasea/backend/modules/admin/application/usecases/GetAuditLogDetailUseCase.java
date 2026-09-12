package com.danasea.backend.modules.admin.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.modules.admin.presentation.dtos.AuditLogResponse;
import com.danasea.backend.modules.audit.domain.models.AuditLog;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAuditLogDetailUseCase {

    private final AuditLogInternalApi auditLogInternalApi;

    public AuditLogResponse execute(UUID id) {
        AuditLog auditLog = auditLogInternalApi.getAuditLogById(id)
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found with id: " + id));
        
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actorUserId(auditLog.getActorUserId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .metadata(auditLog.getMetadata())
                .timestamp(auditLog.getCreatedAt())
                .build();
    }
}
