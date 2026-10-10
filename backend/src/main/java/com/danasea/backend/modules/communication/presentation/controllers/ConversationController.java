package com.danasea.backend.modules.communication.presentation.controllers;

import com.danasea.backend.modules.communication.application.dtos.ConversationResponse;
import com.danasea.backend.modules.communication.application.dtos.CreateConversationRequest;
import com.danasea.backend.modules.communication.application.dtos.CreateOrGetConversationResult;
import com.danasea.backend.modules.communication.application.dtos.MessageResponse;
import com.danasea.backend.modules.communication.application.dtos.SendMessageRequest;
import com.danasea.backend.modules.communication.application.usecases.CreateOrGetConversationUseCase;
import com.danasea.backend.modules.communication.application.usecases.GetConversationsUseCase;
import com.danasea.backend.modules.communication.application.usecases.GetMessagesUseCase;
import com.danasea.backend.modules.communication.application.usecases.SendMessageUseCase;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Validated
public class ConversationController {

    private final CreateOrGetConversationUseCase createOrGetConversationUseCase;
    private final GetConversationsUseCase getConversationsUseCase;
    private final GetMessagesUseCase getMessagesUseCase;
    private final SendMessageUseCase sendMessageUseCase;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversationResponse> createOrGetConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User not authenticated"));
        CreateOrGetConversationResult result = createOrGetConversationUseCase.execute(request, currentUserId);
        HttpStatus status = result.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.conversation());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ConversationResponse>> getConversations(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User not authenticated"));
        Page<ConversationResponse> page = getConversationsUseCase.execute(currentUserId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable("id") UUID id,
            @RequestParam(name = "after", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime after,
            @RequestParam(name = "afterSequence", required = false) Long afterSequence,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User not authenticated"));
        List<MessageResponse> messages = getMessagesUseCase.execute(id, after, afterSequence, pageable, currentUserId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User not authenticated"));
        MessageResponse message = sendMessageUseCase.execute(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
}
