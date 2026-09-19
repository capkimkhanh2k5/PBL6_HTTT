package com.danasea.backend.modules.ai.infrastructure.groq;

import com.danasea.backend.modules.ai.application.port.KeyRotatorPort;
import com.danasea.backend.modules.ai.application.port.LlmClientPort;
import com.danasea.backend.modules.ai.domain.models.AiMessage;
import com.danasea.backend.modules.ai.domain.models.LlmResponse;
import com.danasea.backend.modules.ai.domain.models.ToolCall;
import com.danasea.backend.modules.ai.domain.services.AIToolRegistry;
import com.danasea.backend.modules.ai.infrastructure.groq.config.GroqProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GroqLlmClient implements LlmClientPort {

    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final KeyRotatorPort keyRotator;
    private final AIToolRegistry aiToolRegistry;
    private final String defaultModel;
    private final String fallbackModel;

    @Autowired
    public GroqLlmClient(RestClient.Builder restClientBuilder,
            KeyRotatorPort keyRotator,
            AIToolRegistry aiToolRegistry,
            GroqProperties properties) {
        this(restClientBuilder.baseUrl(properties != null && properties.baseUrl() != null && !properties.baseUrl().isBlank()
                ? properties.baseUrl()
                : DEFAULT_BASE_URL).build(),
                keyRotator,
                aiToolRegistry,
                properties != null ? properties.model() : null,
                properties != null ? properties.fallbackModel() : null);
    }

    public GroqLlmClient(RestClient.Builder restClientBuilder,
            KeyRotatorPort keyRotator,
            AIToolRegistry aiToolRegistry,
            String defaultModel,
            String fallbackModel) {
        this(restClientBuilder.baseUrl(DEFAULT_BASE_URL).build(), keyRotator, aiToolRegistry, defaultModel, fallbackModel);
    }

    public GroqLlmClient(RestClient restClient,
            KeyRotatorPort keyRotator,
            AIToolRegistry aiToolRegistry,
            String defaultModel,
            String fallbackModel) {
        this.restClient = restClient;
        this.keyRotator = keyRotator;
        this.aiToolRegistry = aiToolRegistry;
        this.defaultModel = defaultModel;
        this.fallbackModel = fallbackModel;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    @Override
    public LlmResponse generateResponse(List<AiMessage> messages) {
        return attemptRequest(messages, defaultModel, 0);
    }

    private LlmResponse attemptRequest(List<AiMessage> messages, String model, int attempt) {
        if (attempt > MAX_RETRY_ATTEMPTS) {
            log.warn("Max retry attempts reached for Groq API");
            LlmResponse fallbackResponse = new LlmResponse();
            fallbackResponse.setContent("The AI service is temporarily unavailable. Max retry attempts reached.");
            return fallbackResponse;
        }

        String key;
        try {
            key = keyRotator.getActiveKey();
        } catch (RuntimeException ex) {
            log.error("Failed to obtain active Groq API key (keys exhausted): {}", ex.getMessage());
            LlmResponse fallbackResponse = new LlmResponse();
            fallbackResponse.setContent(
                    "The AI service is temporarily unavailable due to API key exhaustion. Please try again later.");
            return fallbackResponse;
        }

        List<GroqMessage> groqMessages = (messages == null) ? List.of()
                : messages.stream()
                        .map(m -> {
                            String role = m.getRole() != null ? m.getRole().name().toLowerCase() : "user";
                            String content = m.getContent();

                            if ("assistant".equals(role) && m.getToolCalls() != null) {
                                try {
                                    List<ToolCall> tcs = OBJECT_MAPPER.readValue(m.getToolCalls(),
                                            new TypeReference<List<ToolCall>>() {
                                            });
                                    List<GroqToolCall> groqTcs = tcs.stream()
                                            .map(tc -> new GroqToolCall(tc.getId(), "function",
                                                    new GroqFunction(tc.getName(), tc.getArguments())))
                                            .toList();
                                    return new GroqMessage(role, content, groqTcs, null);
                                } catch (Exception e) {
                                    return new GroqMessage(role, content, null, null);
                                }
                            } else if ("tool".equals(role)) {
                                return new GroqMessage(role, content, null, m.getToolCalls());
                            }

                            return new GroqMessage(role, content, null, null);
                        })
                        .toList();

        List<Map<String, Object>> tools = (aiToolRegistry != null && aiToolRegistry.getToolDefinitions() != null)
                ? aiToolRegistry.getToolDefinitions().stream()
                        .map(t -> Map.of("type", "function", "function", t))
                        .toList()
                : List.of();

        GroqRequest request = new GroqRequest(model, groqMessages, tools.isEmpty() ? null : tools);

        try {
            GroqResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + key)
                    .body(request)
                    .retrieve()
                    .body(GroqResponse.class);

            LlmResponse llmResponse = new LlmResponse();
            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                GroqMessage msg = response.choices().get(0).message();
                llmResponse.setContent(msg.content());
                if (key != null && key.length() >= 4) {
                    llmResponse.setKeyMasked("..." + key.substring(key.length() - 4));
                }
                if (msg.tool_calls() != null && !msg.tool_calls().isEmpty()) {
                    List<ToolCall> toolCalls = msg.tool_calls().stream()
                            .map(tc -> new ToolCall(tc.id(), tc.function().name(), tc.function().arguments()))
                            .toList();
                    llmResponse.setToolCalls(toolCalls);
                }
            }
            return llmResponse;
        } catch (ResourceAccessException ex) {
            log.warn("Groq request timed out for model {}: {}", model, ex.getMessage());
            if (!model.equals(fallbackModel)) {
                log.info("Falling back to secondary model {} due to timeout", fallbackModel);
                return attemptRequest(messages, fallbackModel, attempt + 1);
            }
            LlmResponse timeoutResponse = new LlmResponse();
            timeoutResponse.setContent("The AI service timed out. Please try again later.");
            return timeoutResponse;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.warn("Groq API returned HTTP {} for model {}: {}", status, model, ex.getMessage());

            if (status == 429) {
                log.warn("Rate limited (429) by Groq on key, marking cooldown and rotating");
                keyRotator.markKeyCooldown(key);
                return attemptRequest(messages, model, attempt + 1);
            } else if (status == 401 || status == 403) {
                log.error("Authentication failure ({}) on Groq key, marking disabled", status);
                keyRotator.markKeyDisabled(key);
                return attemptRequest(messages, model, attempt + 1);
            } else if (status >= 500 && status < 600) {
                if (!model.equals(fallbackModel)) {
                    log.info("Falling back to secondary model {} due to 5xx error", fallbackModel);
                    return attemptRequest(messages, fallbackModel, attempt + 1);
                }
                LlmResponse serverErrorResponse = new LlmResponse();
                serverErrorResponse.setContent(
                        "The AI service is temporarily unavailable due to upstream server errors. Please try again later.");
                return serverErrorResponse;
            }
            throw ex;
        }
    }

    @JsonInclude(Include.NON_NULL)
    public record GroqMessage(String role, String content, List<GroqToolCall> tool_calls, String tool_call_id) {
        public GroqMessage(String role, String content) {
            this(role, content, null, null);
        }
    }

    public record GroqToolCall(String id, String type, GroqFunction function) {
    }

    public record GroqFunction(String name, String arguments) {
    }

    @JsonInclude(Include.NON_NULL)
    public record GroqRequest(String model, List<GroqMessage> messages, List<Map<String, Object>> tools) {
    }

    public record GroqResponse(List<Choice> choices) {
        public record Choice(GroqMessage message) {
        }
    }
}
