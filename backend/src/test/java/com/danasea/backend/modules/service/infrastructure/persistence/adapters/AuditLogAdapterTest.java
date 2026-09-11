package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogAdapterTest {

    @Mock
    private AccountInternalApi accountInternalApi;

    @InjectMocks
    private AuditLogAdapter adapter;

    @Test
    @DisplayName("recordAuditLog should delegate to accountInternalApi")
    void recordAuditLog_delegates() {
        UUID actorUserId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        String action = "SERVICE_APPROVED";
        String entityType = "SERVICE";
        String metadata = "Approved by admin";

        adapter.recordAuditLog(actorUserId, action, entityType, entityId, metadata);

        verify(accountInternalApi).recordAuditLog(actorUserId, action, entityType, entityId, metadata);
    }
}
