package com.danasea.backend.modules.ai.api.controllers;

import com.danasea.backend.modules.ai.domain.services.ChatHistoryService;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiConversationJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiMessageJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class ChatController {

    private final ChatHistoryService chatHistoryService;

    // TODO: Inject LLM Engine / Google GenAI Service

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> request) {
        // TODO: Rate limiting check (2-layer)
        // 1. Get or Create Conversation
        // 2. Fetch last 5 messages for Context
        // 3. Call LLM Engine
        // 4. Handle Tool Calls & Audit Log
        // 5. Return structured JSON (text + action_cards)
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Chat endpoint initialized. LLM integration pending."
        ));
    }

    @GetMapping("/conversations/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AiMessageJpaEntity>> getConversationHistory(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int limit) {
        
        // Return full (or paginated) persistent history for UI to render
        List<AiMessageJpaEntity> history = chatHistoryService.getRecentMessages(id, limit);
        return ResponseEntity.ok(history);
    }
}
