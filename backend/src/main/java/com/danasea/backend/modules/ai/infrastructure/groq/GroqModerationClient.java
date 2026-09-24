package com.danasea.backend.modules.ai.infrastructure.groq;

import com.danasea.backend.modules.ai.application.port.KeyRotatorPort;
import com.danasea.backend.modules.ai.application.port.ModerationPort;
import com.danasea.backend.configs.properties.HttpClientProperties;
import com.danasea.backend.modules.ai.infrastructure.groq.config.GroqProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
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
    private final RestClient restClient;
    private final RestTemplate legacyRestTemplate;
    private final KeyRotatorPort keyRotator;
    private final String moderationModel;
    private final String groqApiUrl;
    private final ObjectMapper objectMapper;

    @Autowired
    public GroqModerationClient(
            RestClient.Builder restClientBuilder,
            KeyRotatorPort keyRotator,
            GroqProperties properties,
            HttpClientProperties httpClientProperties,
            ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(httpClientProperties.connectTimeout());
        requestFactory.setReadTimeout(httpClientProperties.readTimeout());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.legacyRestTemplate = null;
        this.keyRotator = keyRotator;
        this.moderationModel = properties != null ? properties.moderationModel() : null;
        String baseUrl = properties != null && properties.baseUrl() != null && !properties.baseUrl().isBlank()
                ? properties.baseUrl()
                : DEFAULT_BASE_URL;
        this.groqApiUrl = baseUrl + CHAT_COMPLETIONS_ENDPOINT;
        this.objectMapper = objectMapper;
    }

    public GroqModerationClient(
            KeyRotatorPort keyRotator,
            String moderationModel,
            String groqApiUrl) {
        this(RestClient.create(), keyRotator, moderationModel, groqApiUrl, new ObjectMapper());
    }

    public GroqModerationClient(
            RestTemplate restTemplate,
            KeyRotatorPort keyRotator,
            String moderationModel,
            String groqApiUrl) {
        this.restClient = null;
        this.legacyRestTemplate = java.util.Objects.requireNonNull(restTemplate, "restTemplate");
        this.keyRotator = keyRotator;
        this.moderationModel = moderationModel;
        this.groqApiUrl = groqApiUrl;
        this.objectMapper = new ObjectMapper();
    }

    GroqModerationClient(
            RestClient restClient,
            KeyRotatorPort keyRotator,
            String moderationModel,
            String groqApiUrl,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.legacyRestTemplate = null;
        this.keyRotator = keyRotator;
        this.moderationModel = moderationModel;
        this.groqApiUrl = groqApiUrl;
        this.objectMapper = objectMapper;
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
            return false;
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

        return false;
    }

    @Override
    public boolean isSafe(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }

        try {
            String truncatedText = truncateForModeration(text, DEFAULT_MAX_TOKENS);
            String apiKey = keyRotator.getActiveKey();

            Map<String, Object> requestBody = Map.of(
                    "model", moderationModel,
                    "messages", List.of(
                            Map.of("role", "user", "content", truncatedText)));

            String response;
            if (legacyRestTemplate != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);
                ResponseEntity<String> responseEntity = legacyRestTemplate.postForEntity(
                        groqApiUrl,
                        new HttpEntity<>(requestBody, headers),
                        String.class);
                response = responseEntity.getBody();
            } else {
                response = restClient.post()
                        .uri(groqApiUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers -> headers.setBearerAuth(apiKey))
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);
            }
            if (response != null) {
                JsonNode body = objectMapper.readTree(response);
                if (body.has("choices")) {
                    JsonNode choices = body.get("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        String output = choices.get(0).get("message").get("content").asText();
                        return parseModerationOutput(output);
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Moderation API failed; the request was rejected: {}", e.getMessage());
            return false;
        }
    }
}
