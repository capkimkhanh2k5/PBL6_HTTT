package com.danasea.backend.modules.ai.infrastructure.groq;

import com.danasea.backend.modules.ai.application.port.KeyRotatorPort;
import com.danasea.backend.modules.ai.application.port.ModerationPort;
import com.danasea.backend.modules.ai.infrastructure.groq.config.GroqProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GroqModerationClient implements ModerationPort {

    private static final int DEFAULT_MAX_TOKENS = 450;
    private static final int CHARS_PER_TOKEN = 4;
    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final KeyRotatorPort keyRotator;
    private final String moderationModel;
    private final String groqApiUrl;

    @Autowired
    public GroqModerationClient(
            KeyRotatorPort keyRotator,
            GroqProperties properties,
            @Value("${groq.api.url:}") String groqApiUrlOverride) {
        this(new RestTemplate(), keyRotator,
                properties != null ? properties.moderationModel() : null,
                (groqApiUrlOverride != null && !groqApiUrlOverride.isBlank())
                        ? groqApiUrlOverride
                        : ((properties != null && properties.baseUrl() != null && !properties.baseUrl().isBlank())
                                ? properties.baseUrl()
                                : DEFAULT_BASE_URL) + CHAT_COMPLETIONS_ENDPOINT);
    }

    public GroqModerationClient(
            KeyRotatorPort keyRotator,
            String moderationModel,
            String groqApiUrl) {
        this(new RestTemplate(), keyRotator, moderationModel, groqApiUrl);
    }

    public GroqModerationClient(
            RestTemplate restTemplate,
            KeyRotatorPort keyRotator,
            String moderationModel,
            String groqApiUrl) {
        this.restTemplate = restTemplate;
        this.keyRotator = keyRotator;
        this.moderationModel = moderationModel;
        this.groqApiUrl = groqApiUrl;
    }

    /**
     * Estimates tokens (~4 chars per token) and truncates text if exceeding
     * maxTokens
     * before sending to Llama Prompt Guard (model
     * meta-llama/llama-prompt-guard-2-86m, 512-token limit).
     *
     * @param text      input text to truncate
     * @param maxTokens maximum allowed tokens (default 450)
     * @return truncated text
     */
    public String truncateForModeration(String text, int maxTokens) {
        if (text == null) {
            return null;
        }
        int maxChars = maxTokens * CHARS_PER_TOKEN;
        if (text.length() > maxChars) {
            if (maxChars > 0 && Character.isHighSurrogate(text.charAt(maxChars - 1))) {
                maxChars--;
            }
            log.debug("Truncating input for moderation from {} chars to {} chars (~{} tokens)",
                    text.length(), maxChars, maxTokens);
            return text.substring(0, maxChars);
        }
        return text;
    }

    public String truncateForModeration(String text) {
        return truncateForModeration(text, DEFAULT_MAX_TOKENS);
    }

    /**
     * Parses Llama Prompt Guard response format:
     * - Classification labels: BENIGN vs MALICIOUS / INJECTION / JAILBREAK
     * - Numeric injection probability scores: score > 0.5 indicates injection
     *
     * @param output model response text
     * @return true if safe/benign, false if malicious/injection
     */
    public boolean parseModerationOutput(String output) {
        if (output == null || output.isBlank()) {
            return true;
        }
        String trimmed = output.trim();

        // 1. Check numeric probability score
        try {
            double injectionProbability = Double.parseDouble(trimmed);
            return injectionProbability <= 0.5;
        } catch (NumberFormatException ignored) {
            // Not a pure number, continue to label checks
        }

        // 2. Check classification labels
        String upperOutput = trimmed.toUpperCase();
        if (upperOutput.contains("MALICIOUS") || upperOutput.contains("INJECTION")
                || upperOutput.contains("JAILBREAK") || upperOutput.contains("UNSAFE")) {
            return false;
        }
        if (upperOutput.contains("BENIGN") || upperOutput.contains("SAFE")) {
            return true;
        }

        return true;
    }

    @Override
    public boolean isSafe(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }

        try {
            String truncatedText = truncateForModeration(text, DEFAULT_MAX_TOKENS);
            String apiKey = keyRotator.getActiveKey();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", moderationModel,
                    "messages", List.of(
                            Map.of("role", "user", "content", truncatedText)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(groqApiUrl, entity, String.class);
            if (response.getBody() != null) {
                JsonNode body = OBJECT_MAPPER.readTree(response.getBody());
                if (body.has("choices")) {
                    JsonNode choices = body.get("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        String output = choices.get(0).get("message").get("content").asText();
                        return parseModerationOutput(output);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Moderation API failed, failing open: {}", e.getMessage());
            return true;
        }
    }
}
