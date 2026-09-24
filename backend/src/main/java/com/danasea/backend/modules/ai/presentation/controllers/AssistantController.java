package com.danasea.backend.modules.ai.presentation.controllers;

import com.danasea.backend.modules.ai.application.usecase.ChatUseCase;
import com.danasea.backend.modules.ai.application.usecase.ConfirmBookingUseCase;
import com.danasea.backend.modules.ai.domain.services.AssistantAuditLogService;
import com.danasea.backend.modules.ai.domain.services.ChatHistoryService;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiConversationJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiMessageJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiConversationRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import com.danasea.backend.modules.ai.application.port.RateLimiterPort;
import com.danasea.backend.modules.ai.domain.TrustTier;
import com.danasea.backend.modules.ai.domain.exceptions.AiConversationLocaleMismatchException;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import com.danasea.backend.shared.presentation.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.i18n.LocaleContextHolder;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private static final String DEFAULT_CLIENT_IP = "127.0.0.1";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String DEFAULT_SESSION_PREFIX = "session-";

    private final ChatHistoryService chatHistoryService;
    private final ChatUseCase chatUseCase;
    private final ConfirmBookingUseCase confirmBookingUseCase;
    private final AssistantAuditLogService auditLogService;
    private final JpaAiConversationRepository conversationRepository;
    private final RateLimiterPort rateLimiter;
    private final LocalizedMessageService messages;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        Optional<UUID> currentUserIdOpt = SecurityUtils.getCurrentUserId();
        UUID userId = currentUserIdOpt.orElse(null);
        String userIdStr = userId != null ? userId.toString() : null;
        TrustTier tier = userId != null ? TrustTier.VERIFIED : TrustTier.UNVERIFIED;
        SupportedLanguage language = SupportedLanguage
                .fromTag(LocaleContextHolder.getLocale().toLanguageTag())
                .orElse(SupportedLanguage.VI);

        // Enforce rate limiting prior to calling chatUseCase
        if (!rateLimiter.isAllowed(userIdStr, clientIp, tier)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("RATE_LIMIT_EXCEEDED", messages.get("ai.rate_limit", language)));
        }

        UUID requestedConversationId = request.containsKey("conversationId") && request.get("conversationId") != null
                ? UUID.fromString((String) request.get("conversationId")) : null;
        
        AiConversationJpaEntity conversation = chatHistoryService
                .getOrCreateConversation(requestedConversationId, userId, language);
        SupportedLanguage conversationLanguage = SupportedLanguage
                .fromTag(conversation.getLocale())
                .orElse(SupportedLanguage.VI);
        if (conversationLanguage != language) {
            throw new AiConversationLocaleMismatchException();
        }
        UUID conversationId = conversation.getId();
        
        String userMessage = (String) request.getOrDefault("message", "");
        
        var llmResponse = chatUseCase.processMessage(conversationId, userMessage, conversationLanguage);
        String responseContent = llmResponse.getContent();
        
        // Log chat action
        auditLogService.logChatAction(conversationId, userId, userMessage, responseContent, llmResponse.getKeyMasked());

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "conversationId", conversationId.toString(),
            "message", responseContent
        ));
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return DEFAULT_CLIENT_IP;
        }
        String xf = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : DEFAULT_CLIENT_IP;
    }

    @PostMapping("/conversations/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> confirmBooking(
            @PathVariable UUID id, 
            @RequestBody Map<String, Object> request) {
        String cardId = (String) request.get("cardId");
        
        UUID userId = SecurityUtils.getCurrentUserId().orElseThrow(() -> new IllegalStateException("User not authenticated"));
        String sessionId = request.getOrDefault("sessionId", DEFAULT_SESSION_PREFIX + userId).toString();

        Map<String, Object> result = confirmBookingUseCase.execute(cardId, userId, sessionId);
        
        // Log confirmation action as tool call/chat action
        auditLogService.logToolCall(id, userId, "confirm_booking", 
                "cardId=" + cardId, 
                result.toString());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/conversations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiConversationJpaEntity> getConversation(@PathVariable UUID id) {
        return conversationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/conversations/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AiMessageJpaEntity>> getConversationHistory(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int limit) {
        
        List<AiMessageJpaEntity> history = chatHistoryService.getRecentMessages(id, limit);
        return ResponseEntity.ok(history);
    }
}
