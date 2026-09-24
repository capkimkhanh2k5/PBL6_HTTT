package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.application.port.RateLimiterPort;
import com.danasea.backend.modules.ai.application.usecase.ChatUseCase;
import com.danasea.backend.modules.ai.application.usecase.ConfirmBookingUseCase;
import com.danasea.backend.modules.ai.domain.TrustTier;
import com.danasea.backend.modules.ai.domain.exceptions.AiConversationLocaleMismatchException;
import com.danasea.backend.modules.ai.domain.services.AssistantAuditLogService;
import com.danasea.backend.modules.ai.domain.services.ChatHistoryService;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiConversationJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiConversationRepository;
import com.danasea.backend.modules.ai.presentation.controllers.AssistantController;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantControllerLocaleTest {

    @Mock private ChatHistoryService chatHistoryService;
    @Mock private ChatUseCase chatUseCase;
    @Mock private ConfirmBookingUseCase confirmBookingUseCase;
    @Mock private AssistantAuditLogService auditLogService;
    @Mock private JpaAiConversationRepository conversationRepository;
    @Mock private RateLimiterPort rateLimiter;

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void chat_rejectsRequestLocaleThatDiffersFromLockedConversationLocale() {
        UUID conversationId = UUID.randomUUID();
        AiConversationJpaEntity conversation = new AiConversationJpaEntity();
        conversation.setId(conversationId);
        conversation.setLocale("vi");
        when(rateLimiter.isAllowed(null, "127.0.0.1", TrustTier.UNVERIFIED)).thenReturn(true);
        when(chatHistoryService.getOrCreateConversation(eq(conversationId), eq(null), any()))
                .thenReturn(conversation);
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        AssistantController controller = new AssistantController(
                chatHistoryService,
                chatUseCase,
                confirmBookingUseCase,
                auditLogService,
                conversationRepository,
                rateLimiter,
                LocalizedMessageService.standalone());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThrows(AiConversationLocaleMismatchException.class, () -> controller.chat(
                Map.of("conversationId", conversationId.toString(), "message", "Hello"), request));
        verify(chatUseCase, never()).processMessage(any(), any(), any());
    }
}
