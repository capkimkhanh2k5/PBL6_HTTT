package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.domain.services.AssistantAuditLogService;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AssistantAuditLogJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAssistantAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Acceptance Criteria & 4-Tier Test Suite for AssistantAuditLogTest.
 *
 * <p>Requirements:
 * - R5: Add `key_masked` field (last 4 characters) to `assistant_audit_log` and `tool_call_log` for billing trace.
 * - R5: Ensure moderation-blocked cases and tool executions are successfully logged.
 * - R5: Verifies audit log repository persistence and payload fidelity.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssistantAuditLog Acceptance Test Suite (Tiers 1-4)")
public class AssistantAuditLogTest {

    @Mock
    private JpaAssistantAuditLogRepository auditLogRepository;

    private AssistantAuditLogService auditLogService;

    private final UUID conversationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        auditLogService = new AssistantAuditLogService(auditLogRepository);
    }

    // =========================================================================
    // TIER 1: FEATURE COVERAGE (>=5 test cases across R5 core requirements)
    // =========================================================================

    @Test
    @DisplayName("Tier 1 - F5.1: logChatAction correctly records key_masked for billing trace")
    void testLogChatAction_RecordsKeyMasked() {
        String requestMsg = "Tìm cho tôi tour đi Cù Lao Chàm";
        String responseMsg = "Dưới đây là các tour đi Cù Lao Chàm...";
        String maskedKey = "...3456";

        auditLogService.logChatAction(conversationId, userId, requestMsg, responseMsg, maskedKey);

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AssistantAuditLogJpaEntity saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(conversationId, saved.getConversationId());
        assertEquals(userId, saved.getUserId());
        assertEquals("chat", saved.getToolName());
        assertEquals("...3456", saved.getKeyMasked());
        assertEquals(requestMsg, saved.getRequestPayload());
        assertEquals(responseMsg, saved.getResponsePayload());
        assertNotNull(saved.getExecutedAt());
    }

    @Test
    @DisplayName("Tier 1 - F5.2: logToolCall persists toolName, requestPayload, and responsePayload")
    void testLogToolCall_PersistsToolExecution() {
        String toolName = "search_service";
        String reqPayload = "{\"keyword\":\"lặn ngắm san hô\"}";
        String respPayload = "{\"results\": 3}";

        auditLogService.logToolCall(conversationId, userId, toolName, reqPayload, respPayload);

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AssistantAuditLogJpaEntity saved = captor.getValue();
        assertEquals(conversationId, saved.getConversationId());
        assertEquals(userId, saved.getUserId());
        assertEquals("search_service", saved.getToolName());
        assertEquals(reqPayload, saved.getRequestPayload());
        assertEquals(respPayload, saved.getResponsePayload());
        assertNotNull(saved.getExecutedAt());
    }

    @Test
    @DisplayName("Tier 1 - F5.3: Moderation-blocked event is successfully logged to audit log")
    void testLogModerationBlockedEvent_PersistedInAuditLog() {
        String maliciousPrompt = "DROP TABLE services; --";
        String blockReason = "BLOCKED: Prompt injection detected";

        auditLogService.logChatAction(conversationId, userId, maliciousPrompt, blockReason, null);

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AssistantAuditLogJpaEntity saved = captor.getValue();
        assertEquals(maliciousPrompt, saved.getRequestPayload());
        assertEquals(blockReason, saved.getResponsePayload());
        assertNull(saved.getKeyMasked(), "No LLM key should be recorded when request is blocked before LLM");
    }

    @Test
    @DisplayName("Tier 1 - F5.4: Booking confirmation tool execution is logged with card ID")
    void testLogBookingConfirmationAction_PersistedInAuditLog() {
        String reqPayload = "cardId=card-999-confirm";
        String respPayload = "{\"status\":\"success\",\"message\":\"Booking confirmed\"}";

        auditLogService.logToolCall(conversationId, userId, "confirm_booking", reqPayload, respPayload);

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AssistantAuditLogJpaEntity saved = captor.getValue();
        assertEquals("confirm_booking", saved.getToolName());
        assertTrue(saved.getRequestPayload().contains("card-999-confirm"));
        assertTrue(saved.getResponsePayload().contains("Booking confirmed"));
    }

    @Test
    @DisplayName("Tier 1 - F5.5: Policy inquiry tool call is logged with policy_type parameter")
    void testLogPolicyToolCall_PersistedInAuditLog() {
        String reqPayload = "{\"policy_type\":\"CANCELLATION\"}";
        String respPayload = "{\"text\":\"Hoàn 100% trước 24h\"}";

        auditLogService.logToolCall(conversationId, userId, "get_policy", reqPayload, respPayload);

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AssistantAuditLogJpaEntity saved = captor.getValue();
        assertEquals("get_policy", saved.getToolName());
        assertTrue(saved.getRequestPayload().contains("CANCELLATION"));
    }

    // =========================================================================
    // TIER 2: BOUNDARY & CORNER CASES (>=5 test cases)
    // =========================================================================

    @Test
    @DisplayName("Tier 2 - B5.1: Null userId for anonymous guest session is recorded safely")
    void testLogChatAction_AnonymousUser_NullUserId() {
        auditLogService.logChatAction(conversationId, null, "Hello", "Hi", "...1234");

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        assertNull(captor.getValue().getUserId(), "Anonymous requests should store null userId without error");
    }

    @Test
    @DisplayName("Tier 2 - B5.2: Empty request and response payloads handled safely")
    void testLogChatAction_EmptyPayloads() {
        auditLogService.logChatAction(conversationId, userId, "", "", "...9999");

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        assertEquals("", captor.getValue().getRequestPayload());
        assertEquals("", captor.getValue().getResponsePayload());
    }

    @Test
    @DisplayName("Tier 2 - B5.3: Large payload (> 10,000 characters) recorded accurately")
    void testLogChatAction_LargePayload() {
        String largeMsg = "A".repeat(15000);
        auditLogService.logChatAction(conversationId, userId, largeMsg, "Short response", "...5678");

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        assertEquals(15000, captor.getValue().getRequestPayload().length());
    }

    @Test
    @DisplayName("Tier 2 - B5.4: Special characters, JSON formatting and Unicode in payloads are preserved verbatim")
    void testLogChatAction_SpecialCharactersAndUnicode() {
        String unicodeMsg = "Tour lặn biển Đà Nẵng 🌊🐠! Giá: 1.500.000₫ & <xml>test</xml>";
        auditLogService.logChatAction(conversationId, userId, unicodeMsg, "OK", "...7890");

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        assertEquals(unicodeMsg, captor.getValue().getRequestPayload());
    }

    @Test
    @DisplayName("Tier 2 - B5.5: ExecutedAt timestamp is set to recent UTC/local offset time")
    void testLogChatAction_ExecutedAtTimestampValid() {
        OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);

        auditLogService.logChatAction(conversationId, userId, "Hi", "Hello", "...1111");

        OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        OffsetDateTime executedAt = captor.getValue().getExecutedAt();
        assertNotNull(executedAt);
        assertTrue(executedAt.isAfter(before) || executedAt.isEqual(before));
        assertTrue(executedAt.isBefore(after) || executedAt.isEqual(after));
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE COMBINATIONS
    // =========================================================================

    @Test
    @DisplayName("Tier 3 - C5.1: Pairwise tool call sequence: search -> detail -> confirmation audit trail")
    void testSequentialToolAuditTrail_MaintainsCorrectConversationId() {
        auditLogService.logToolCall(conversationId, userId, "search_service", "{\"q\":\"cano\"}", "{\"count\":1}");
        auditLogService.logToolCall(conversationId, userId, "get_service_detail", "{\"id\":\"srv-1\"}", "{\"price\":500000}");
        auditLogService.logToolCall(conversationId, userId, "confirm_booking", "cardId=c-1", "{\"status\":\"success\"}");

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository, times(3)).save(captor.capture());

        var logs = captor.getAllValues();
        assertEquals(3, logs.size());
        assertEquals("search_service", logs.get(0).getToolName());
        assertEquals("get_service_detail", logs.get(1).getToolName());
        assertEquals("confirm_booking", logs.get(2).getToolName());

        for (AssistantAuditLogJpaEntity log : logs) {
            assertEquals(conversationId, log.getConversationId());
            assertEquals(userId, log.getUserId());
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD APPLICATION SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Tier 4 - R5.1: Billing audit forensics - key_masked enables tracing exact API key used for invoice dispute")
    void testBillingAuditForensics_KeyMaskedMatchesActiveRotatedKey() {
        String key1Masked = "...1234";
        String key2Masked = "...5678";

        auditLogService.logChatAction(conversationId, userId, "Question 1", "Answer 1", key1Masked);
        auditLogService.logChatAction(conversationId, userId, "Question 2", "Answer 2", key2Masked);

        ArgumentCaptor<AssistantAuditLogJpaEntity> captor = ArgumentCaptor.forClass(AssistantAuditLogJpaEntity.class);
        verify(auditLogRepository, times(2)).save(captor.capture());

        var savedLogs = captor.getAllValues();
        assertEquals("...1234", savedLogs.get(0).getKeyMasked());
        assertEquals("...5678", savedLogs.get(1).getKeyMasked());
    }
}
