package com.danasea.backend.modules.audit.application.port;

import com.danasea.backend.modules.audit.domain.models.AuditLog;

public interface AuditLogPort {
    void saveAuditLog(AuditLog log);
}
