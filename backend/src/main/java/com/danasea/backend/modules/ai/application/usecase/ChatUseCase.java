package com.danasea.backend.modules.ai.application.usecase;

import com.danasea.backend.modules.ai.application.port.LlmClientPort;
import com.danasea.backend.modules.ai.application.port.ModerationPort;
import com.danasea.backend.modules.ai.domain.models.AiMessageRole;
import com.danasea.backend.modules.ai.domain.models.LlmResponse;
import com.danasea.backend.modules.ai.domain.models.ToolCall;
import com.danasea.backend.modules.ai.domain.services.ChatHistoryService;
import com.danasea.backend.modules.ai.application.tool.ToolExecutor;
import com.danasea.backend.modules.ai.application.tool.ConversationAwareToolExecutor;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiMessageJpaEntity;
import com.danasea.backend.modules.ai.domain.models.AiMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.danasea.backend.modules.ai.domain.services.AssistantAuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatUseCase {

    private static final List<String> POLICY_KEYWORDS = List.of(
            "hoàn tiền",
            "chính sách hủy",
            "chính sách hoãn",
            "quy định hoàn",
            "quy định hủy",
            "cancellation policy",
            "refund policy"
    );
    private static final int MAX_USER_MESSAGE_CHARS = 1800;
    private static final int MAX_CHAT_ITERATIONS = 5;
    private static final int DEFAULT_HISTORY_LIMIT = 20;

    private final LlmClientPort llmClientPort;
    private final ModerationPort moderationPort;
    private final ChatHistoryService chatHistoryService;
    private final Map<String, ToolExecutor> toolExecutors;
    private final ObjectMapper objectMapper;
    private final AssistantAuditLogService auditLogService;

    @Autowired
    public ChatUseCase(LlmClientPort llmClientPort, 
                       ModerationPort moderationPort, 
                       ChatHistoryService chatHistoryService, 
                       List<ToolExecutor> tools,
                       ObjectMapper objectMapper,
                       @Autowired(required = false) AssistantAuditLogService auditLogService) {
        this.llmClientPort = llmClientPort;
        this.moderationPort = moderationPort;
        this.chatHistoryService = chatHistoryService;
        this.toolExecutors = tools.stream().collect(Collectors.toMap(ToolExecutor::getName, t -> t));
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
    }

    public ChatUseCase(LlmClientPort llmClientPort, 
                       ModerationPort moderationPort, 
                       ChatHistoryService chatHistoryService, 
                       List<ToolExecutor> tools,
                       ObjectMapper objectMapper) {
        this(llmClientPort, moderationPort, chatHistoryService, tools, objectMapper, null);
    }

    public LlmResponse processMessage(UUID conversationId, String userMessageContent) {
        String effectiveContent = userMessageContent;
        if (effectiveContent != null && effectiveContent.length() > MAX_USER_MESSAGE_CHARS) {
            int maxChars = MAX_USER_MESSAGE_CHARS;
            if (Character.isHighSurrogate(effectiveContent.charAt(maxChars - 1))) {
                maxChars--;
            }
            effectiveContent = effectiveContent.substring(0, maxChars);
        }

        if (!moderationPort.isSafe(effectiveContent)) {
            if (auditLogService != null) {
                try {
                    auditLogService.logChatAction(conversationId, null, userMessageContent, "BLOCKED: Prompt injection detected", null);
                } catch (Exception e) {
                    log.error("Failed to log moderation blocked event", e);
                }
            }
            throw new IllegalArgumentException("Message violates safety policies or contains prompt injections.");
        }

        chatHistoryService.appendMessage(conversationId, AiMessageRole.USER, effectiveContent, null);

        return executeChatLoop(conversationId);
    }

    private LlmResponse executeChatLoop(UUID conversationId) {
        LlmResponse lastResponse = null;
        boolean policyToolCalled = false;

        for (int i = 0; i < MAX_CHAT_ITERATIONS; i++) {
            List<AiMessageJpaEntity> recentJpa = chatHistoryService.getRecentMessages(conversationId, DEFAULT_HISTORY_LIMIT);
            List<AiMessage> history = mapToDomain(recentJpa);

            LlmResponse response = llmClientPort.generateResponse(history);
            lastResponse = response;

            if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
                try {
                    String toolCallsStr = objectMapper.writeValueAsString(response.getToolCalls());
                    chatHistoryService.appendMessage(conversationId, AiMessageRole.ASSISTANT, response.getContent(), toolCallsStr);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Error serializing tool calls", e);
                }

                for (ToolCall tc : response.getToolCalls()) {
                    if ("get_policy".equals(tc.getName()) || "get_cancellation_policy".equals(tc.getName())) {
                        policyToolCalled = true;
                    }
                    ToolExecutor executor = toolExecutors.get(tc.getName());
                    String result;
                    if (executor != null) {
                        result = executor instanceof ConversationAwareToolExecutor conversationAwareTool
                                ? conversationAwareTool.execute(tc.getArguments(), conversationId)
                                : executor.execute(tc.getArguments());
                    } else {
                        result = "{\"error\": \"Unknown tool\"}";
                    }
                    // Append tool response
                    chatHistoryService.appendMessage(conversationId, AiMessageRole.TOOL, result, tc.getId());
                }
            } else {
                // If response mentions policy keywords but get_policy was never called, reject hallucinated policy
                if (!policyToolCalled && containsPolicyKeywords(response.getContent())) {
                    String rejectionMessage = "I cannot provide cancellation or refund policy details without consulting the official policy source. Please ask me to check a specific policy.";
                    response.setContent(rejectionMessage);
                }

                chatHistoryService.appendMessage(conversationId, AiMessageRole.ASSISTANT, response.getContent(), null);
                return response;
            }
        }
        LlmResponse timeoutResponse = new LlmResponse();
        timeoutResponse.setContent("Sorry, I am taking too long to process your request.");
        timeoutResponse.setKeyMasked(lastResponse != null ? lastResponse.getKeyMasked() : null);
        return timeoutResponse;
    }

    private boolean containsPolicyKeywords(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String lower = content.toLowerCase();
        for (String kw : POLICY_KEYWORDS) {
            if (lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private List<AiMessage> mapToDomain(List<AiMessageJpaEntity> jpaList) {
        List<AiMessageJpaEntity> modifiableList = new ArrayList<>(jpaList);
        Collections.reverse(modifiableList);
        return modifiableList.stream().map(jpa -> {
            AiMessage msg = new AiMessage();
            msg.setConversationId(jpa.getConversationId());
            msg.setRole(jpa.getRole());
            msg.setContent(jpa.getContent());
            msg.setToolCalls(jpa.getToolCalls());
            return msg;
        }).collect(Collectors.toList());
    }
}
