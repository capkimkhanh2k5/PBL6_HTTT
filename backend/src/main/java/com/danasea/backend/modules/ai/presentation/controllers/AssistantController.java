package com.danasea.backend.modules.ai.presentation.controllers;

import com.danasea.backend.modules.ai.application.usecase.ChatUseCase;
import com.danasea.backend.modules.ai.application.usecase.ConfirmBookingUseCase;
import com.danasea.backend.modules.ai.domain.services.AssistantAuditLogService;
import com.danasea.backend.modules.ai.domain.services.ChatHistoryService;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiConversationJpaEntity;
import com.danasea.backend.modules.ai.presentation.dtos.ChatRequest;
import com.danasea.backend.modules.ai.presentation.dtos.ChatResponse;
import com.danasea.backend.modules.ai.presentation.dtos.ConfirmConversationRequest;
import com.danasea.backend.modules.ai.presentation.dtos.ConversationResponse;
import com.danasea.backend.modules.ai.presentation.dtos.MessageResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import com.danasea.backend.modules.ai.application.port.RateLimiterPort;
import com.danasea.backend.modules.ai.domain.TrustTier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final RateLimiterPort rateLimiter;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        UUID userId = currentUserId();
        String userIdStr = userId.toString();
        TrustTier tier = TrustTier.VERIFIED;

        // Enforce rate limiting prior to calling chatUseCase
        if (!rateLimiter.isAllowed(userIdStr, clientIp, tier)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "status", "error",
                "message", "Rate limit exceeded. Please try again later."
            ));
        }

        AiConversationJpaEntity conversation = chatHistoryService.getOrCreateConversation(request.conversationId(), userId);
        UUID conversationId = conversation.getId();

        String userMessage = request.message();
        
        var llmResponse = chatUseCase.processMessage(conversationId, userMessage);
        String responseContent = llmResponse.getContent();
        
        // Log chat action
        auditLogService.logChatAction(conversationId, userId, userMessage, responseContent, llmResponse.getKeyMasked());

        return ResponseEntity.ok(new ChatResponse("success", conversationId, responseContent));
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
            @Valid @RequestBody ConfirmConversationRequest request) {
        UUID userId = currentUserId();
        chatHistoryService.getConversationForUser(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? DEFAULT_SESSION_PREFIX + userId
                : request.sessionId();

        Map<String, Object> result = confirmBookingUseCase.execute(request.cardId(), userId, sessionId, id);
        
        // Log confirmation action as tool call/chat action
        auditLogService.logToolCall(id, userId, "confirm_booking", 
                "cardId=" + request.cardId(),
                result.toString());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/conversations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversationResponse> getConversation(@PathVariable UUID id) {
        UUID userId = currentUserId();
        return chatHistoryService.getConversationForUser(id, userId)
                .map(ConversationResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/conversations/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageResponse>> getConversationHistory(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        UUID userId = currentUserId();
        return chatHistoryService.getRecentMessagesForUser(id, userId, limit)
                .map(messages -> messages.stream().map(MessageResponse::from).toList())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));
    }
}
