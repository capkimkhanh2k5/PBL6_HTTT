package com.danasea.backend.modules.audit.infrastructure.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import com.danasea.backend.modules.audit.application.port.AuditLogPort;
import com.danasea.backend.modules.audit.domain.models.AuditLog;
import com.danasea.backend.modules.audit.infrastructure.mapper.AuditLogMapper;
import com.danasea.backend.modules.audit.infrastructure.persistence.repositories.JpaAuditLogRepository;
import com.danasea.backend.modules.audit.infrastructure.persistence.entities.AuditLogJpaEntity;

@Component
@RequiredArgsConstructor
public class AuditLogAdapter implements AuditLogPort {
    private final JpaAuditLogRepository jpaAuditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    public void saveAuditLog(AuditLog log) {
        AuditLogJpaEntity entity = auditLogMapper.toEntity(log);
        jpaAuditLogRepository.save(entity);
    }

    @Override
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return jpaAuditLogRepository.findAll(pageable)
                .map(auditLogMapper::toDomain);
    }

    @Override
    public Optional<AuditLog> getAuditLogById(UUID id) {
        return jpaAuditLogRepository.findById(id)
                .map(auditLogMapper::toDomain);
    }
}
