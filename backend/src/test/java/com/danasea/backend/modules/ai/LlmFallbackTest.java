package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.application.port.KeyRotatorPort;
import com.danasea.backend.modules.ai.domain.models.AiMessage;
import com.danasea.backend.modules.ai.domain.models.AiMessageRole;
import com.danasea.backend.modules.ai.domain.models.LlmResponse;
import com.danasea.backend.modules.ai.domain.services.AIToolRegistry;
import com.danasea.backend.modules.ai.infrastructure.groq.GroqLlmClient;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Acceptance Criteria & 4-Tier Test Suite for LlmFallbackTest.
 *
 * <p>Requirements:
 * - R5: Verifies fallback behavior upon main model timeout/5xx and when all keys are exhausted.
 * - R5: Rotates key on 429 (marks COOLDOWN) and 401 (marks DISABLED).
 * - R5: Enforces max retry attempts limit.
 * - R5: Returns key_masked in response for billing trace.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LlmFallback Acceptance Test Suite (Tiers 1-4)")
public class LlmFallbackTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private KeyRotatorPort keyRotator;

    @Mock
    private AIToolRegistry aiToolRegistry;

    private GroqLlmClient groqLlmClient;

    private final String defaultModel = "llama-3.3-70b-versatile";
    private final String fallbackModel = "llama-3.1-8b-instant";

    @BeforeEach
    void setUp() {
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        lenient().when(aiToolRegistry.getToolDefinitions()).thenReturn(Collections.emptyList());

        groqLlmClient = new GroqLlmClient(
                restClient,
                keyRotator,
                aiToolRegistry,
                defaultModel,
                fallbackModel
        );
    }

    // =========================================================================
    // TIER 1: FEATURE COVERAGE (>=5 test cases across R5 core requirements)
    // =========================================================================

    @Test
    @DisplayName("i18n: Groq request includes the conversation language system prompt")
    void localizedRequest_includesLanguageRuleInSystemPrompt() {
        when(keyRotator.getActiveKey()).thenReturn("gsk_locale_test_1234");
        GroqLlmClient.GroqMessage responseMessage = new GroqLlmClient.GroqMessage("assistant", "Hello");
        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class)))
                .thenReturn(new GroqLlmClient.GroqResponse(List.of(
                        new GroqLlmClient.GroqResponse.Choice(responseMessage))));

        groqLlmClient.generateResponse(List.of(), SupportedLanguage.EN);

        ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodySpec).body(requestCaptor.capture());
        GroqLlmClient.GroqRequest request = (GroqLlmClient.GroqRequest) requestCaptor.getValue();
        assertEquals("system", request.messages().getFirst().role());
        assertTrue(request.messages().getFirst().content()
                .contains("Answer every user-facing response in English."));
    }

    @Test
    @DisplayName("Tier 1 - F5.1: Primary model succeeds on first attempt with key_masked populated")
    void testPrimaryModel_Success_ReturnsMaskedKey() {
        when(keyRotator.getActiveKey()).thenReturn("gsk_secret_token_1234");

        GroqLlmClient.GroqMessage message = new GroqLlmClient.GroqMessage("assistant", "Xin chào! Tôi có thể giúp gì cho bạn?");
        GroqLlmClient.GroqResponse.Choice choice = new GroqLlmClient.GroqResponse.Choice(message);
        GroqLlmClient.GroqResponse groqResponse = new GroqLlmClient.GroqResponse(List.of(choice));

        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class))).thenReturn(groqResponse);

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Xin chào");

        LlmResponse response = groqLlmClient.generateResponse(List.of(userMsg));

        assertNotNull(response);
        assertEquals("Xin chào! Tôi có thể giúp gì cho bạn?", response.getContent());
        assertEquals("...1234", response.getKeyMasked());
        verify(keyRotator, times(1)).getActiveKey();
    }

    @Test
    @DisplayName("Tier 1 - F5.2: 5xx Server Error on primary model triggers fallback to fallbackModel")
    void testServerError5xx_TriggersFallbackModel() {
        when(keyRotator.getActiveKey()).thenReturn("gsk_key_1", "gsk_key_1");

        RestClientResponseException server500Exception = new RestClientResponseException(
                "Internal Server Error",
                HttpStatusCode.valueOf(500),
                "500 Internal Server Error",
                null, null, null
        );

        GroqLlmClient.GroqMessage fallbackMsg = new GroqLlmClient.GroqMessage("assistant", "Phản hồi từ mô hình dự phòng");
        GroqLlmClient.GroqResponse.Choice choice = new GroqLlmClient.GroqResponse.Choice(fallbackMsg);
        GroqLlmClient.GroqResponse fallbackGroqResponse = new GroqLlmClient.GroqResponse(List.of(choice));

        // First call fails with 500 on primary model; second call succeeds with fallback model
        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class)))
                .thenThrow(server500Exception)
                .thenReturn(fallbackGroqResponse);

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Test 500 error fallback");

        LlmResponse response = groqLlmClient.generateResponse(List.of(userMsg));

        assertNotNull(response);
        assertEquals("Phản hồi từ mô hình dự phòng", response.getContent());
        // Verify two attempts were made
        verify(restClient, times(2)).post();
    }

    @Test
    @DisplayName("Tier 1 - F5.3: 429 Too Many Requests marks key as COOLDOWN and retries with next key")
    void testRateLimit429_MarksCooldownAndRotatesKey() {
        when(keyRotator.getActiveKey())
                .thenReturn("gsk_key_cooldown_1")
                .thenReturn("gsk_key_active_2");

        RestClientResponseException rateLimit429 = new RestClientResponseException(
                "Too Many Requests",
                HttpStatusCode.valueOf(429),
                "Rate limit exceeded",
                null, null, null
        );

        GroqLlmClient.GroqMessage successMsg = new GroqLlmClient.GroqMessage("assistant", "Thành công với key 2");
        GroqLlmClient.GroqResponse.Choice choice = new GroqLlmClient.GroqResponse.Choice(successMsg);
        GroqLlmClient.GroqResponse groqResponse = new GroqLlmClient.GroqResponse(List.of(choice));

        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class)))
                .thenThrow(rateLimit429)
                .thenReturn(groqResponse);

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Test 429 rate limit");

        LlmResponse response = groqLlmClient.generateResponse(List.of(userMsg));

        assertNotNull(response);
        assertEquals("Thành công với key 2", response.getContent());
        assertEquals("...ve_2", response.getKeyMasked());
        // Verify key 1 was marked as COOLDOWN
        verify(keyRotator).markKeyCooldown("gsk_key_cooldown_1");
        // Verify 2 attempts were made
        verify(restClient, times(2)).post();
    }

    @Test
    @DisplayName("Tier 1 - F5.4: 401 Unauthorized marks key as DISABLED and retries with next key")
    void testUnauthorized401_MarksDisabledAndRotatesKey() {
        when(keyRotator.getActiveKey())
                .thenReturn("gsk_key_bad_1")
                .thenReturn("gsk_key_good_2");

        RestClientResponseException auth401 = new RestClientResponseException(
                "Unauthorized",
                HttpStatusCode.valueOf(401),
                "Invalid API Key",
                null, null, null
        );

        GroqLlmClient.GroqMessage successMsg = new GroqLlmClient.GroqMessage("assistant", "Thành công với key hợp lệ");
        GroqLlmClient.GroqResponse.Choice choice = new GroqLlmClient.GroqResponse.Choice(successMsg);
        GroqLlmClient.GroqResponse groqResponse = new GroqLlmClient.GroqResponse(List.of(choice));

        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class)))
                .thenThrow(auth401)
                .thenReturn(groqResponse);

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Test 401 unauthorized");

        LlmResponse response = groqLlmClient.generateResponse(List.of(userMsg));

        assertNotNull(response);
        assertEquals("Thành công với key hợp lệ", response.getContent());
        // Verify key 1 was marked as DISABLED
        verify(keyRotator).markKeyDisabled("gsk_key_bad_1");
        verify(restClient, times(2)).post();
    }

    @Test
    @DisplayName("Tier 1 - F5.5: All keys exhausted returns fallback unavailable response")
    void testAllKeysExhausted_ReturnsUnavailableResponse() {
        when(keyRotator.getActiveKey()).thenThrow(
                new RuntimeException("All Groq API keys are currently COOLDOWN or DISABLED")
        );

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Test key exhaustion");

        LlmResponse response = groqLlmClient.generateResponse(List.of(userMsg));

        assertNotNull(response);
        assertTrue(response.getContent().contains("temporarily unavailable"));
        verifyNoInteractions(restClient);
    }

    // =========================================================================
    // TIER 2: BOUNDARY & CORNER CASES (>=5 test cases)
    // =========================================================================

    @Test
    @DisplayName("Tier 2 - B5.1: Max retry attempts limit (> 3 retries) terminates safely with message")
    void testMaxRetriesExceeded_TerminatesSafely() {
        when(keyRotator.getActiveKey()).thenReturn("gsk_key_spam");

        RestClientResponseException rateLimit429 = new RestClientResponseException(
                "429", HttpStatusCode.valueOf(429), "429", null, null, null
        );

        // Always throws 429
        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class))).thenThrow(rateLimit429);

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Test max retries");

        LlmResponse response = groqLlmClient.generateResponse(List.of(userMsg));

        assertNotNull(response);
        assertTrue(response.getContent().contains("temporarily unavailable"));
        // Verify attempts 0, 1, 2, 3 were made (4 requests total), then attempt 4 terminates
        verify(restClient, times(4)).post();
    }

    @Test
    @DisplayName("Tier 2 - B5.2: Fallback model also returns 5xx - returns server error notice without loop")
    void testFallbackModelAlsoFails5xx_ReturnsServerErrorNotice() {
        when(keyRotator.getActiveKey()).thenReturn("gsk_key_1");

        RestClientResponseException server503 = new RestClientResponseException(
                "Service Unavailable", HttpStatusCode.valueOf(503), "503", null, null, null
        );

        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class))).thenThrow(server503);

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Test double 5xx failure");

        LlmResponse response = groqLlmClient.generateResponse(List.of(userMsg));

        assertNotNull(response);
        assertTrue(response.getContent().contains("temporarily unavailable"));
        // 1st call on defaultModel, 2nd call on fallbackModel -> terminates
        verify(restClient, times(2)).post();
    }

    @Test
    @DisplayName("Tier 2 - B5.3: 400 Bad Request exception is re-thrown immediately without retry")
    void testBadRequest400_RethrowsImmediatelyWithoutRetry() {
        when(keyRotator.getActiveKey()).thenReturn("gsk_key_1");

        RestClientResponseException badRequest400 = new RestClientResponseException(
                "Bad Request", HttpStatusCode.valueOf(400), "Bad request payload", null, null, null
        );

        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class))).thenThrow(badRequest400);

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Malformed payload");

        assertThrows(RestClientResponseException.class, () -> {
            groqLlmClient.generateResponse(List.of(userMsg));
        });

        // Verify only 1 attempt was made; no wasteful retry for client errors
        verify(restClient, times(1)).post();
        verify(keyRotator, never()).markKeyCooldown(anyString());
        verify(keyRotator, never()).markKeyDisabled(anyString());
    }

    @Test
    @DisplayName("Tier 2 - B5.4: Model timeout (ResourceAccessException) triggers fallback to fallbackModel")
    void testModelTimeout_TriggersFallbackModel() {
        when(keyRotator.getActiveKey()).thenReturn("gsk_key_timeout");

        ResourceAccessException timeoutEx = new ResourceAccessException("Read timed out after 10000ms");

        GroqLlmClient.GroqMessage fallbackMsg = new GroqLlmClient.GroqMessage("assistant", "Phản hồi sau khi timeout");
        GroqLlmClient.GroqResponse.Choice choice = new GroqLlmClient.GroqResponse.Choice(fallbackMsg);
        GroqLlmClient.GroqResponse fallbackGroqResponse = new GroqLlmClient.GroqResponse(List.of(choice));

        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class)))
                .thenThrow(timeoutEx)
                .thenReturn(fallbackGroqResponse);

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Test timeout fallback");

        LlmResponse response = groqLlmClient.generateResponse(List.of(userMsg));

        assertNotNull(response);
        assertEquals("Phản hồi sau khi timeout", response.getContent());
        verify(restClient, times(2)).post();
    }

    @Test
    @DisplayName("Tier 2 - B5.5: Empty messages list handled safely")
    void testEmptyMessagesList_HandledSafely() {
        when(keyRotator.getActiveKey()).thenReturn("gsk_key_1");

        GroqLlmClient.GroqMessage msg = new GroqLlmClient.GroqMessage("assistant", "Hello");
        GroqLlmClient.GroqResponse groqResponse = new GroqLlmClient.GroqResponse(List.of(new GroqLlmClient.GroqResponse.Choice(msg)));
        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class))).thenReturn(groqResponse);

        LlmResponse resp = groqLlmClient.generateResponse(Collections.emptyList());
        assertNotNull(resp);
        assertEquals("Hello", resp.getContent());
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE COMBINATIONS
    // =========================================================================

    @Test
    @DisplayName("Tier 3 - C5.1: 429 on Key 1 rotates to Key 2 which then experiences 500 and falls back to secondary model")
    void testComplexFailureCascade_429Then500ThenFallbackSuccess() {
        when(keyRotator.getActiveKey())
                .thenReturn("gsk_key_1")
                .thenReturn("gsk_key_2")
                .thenReturn("gsk_key_2");

        RestClientResponseException rateLimit429 = new RestClientResponseException(
                "429", HttpStatusCode.valueOf(429), "429", null, null, null
        );
        RestClientResponseException server500 = new RestClientResponseException(
                "500", HttpStatusCode.valueOf(500), "500", null, null, null
        );

        GroqLlmClient.GroqMessage successMsg = new GroqLlmClient.GroqMessage("assistant", "Thành công ở model phụ");
        GroqLlmClient.GroqResponse groqResponse = new GroqLlmClient.GroqResponse(List.of(new GroqLlmClient.GroqResponse.Choice(successMsg)));

        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class)))
                .thenThrow(rateLimit429)     // Attempt 0 on defaultModel with key 1 -> 429
                .thenThrow(server500)        // Attempt 1 on defaultModel with key 2 -> 500
                .thenReturn(groqResponse);   // Attempt 2 on fallbackModel with key 2 -> success

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Complex cascade test");

        LlmResponse response = groqLlmClient.generateResponse(List.of(userMsg));

        assertNotNull(response);
        assertEquals("Thành công ở model phụ", response.getContent());
        verify(keyRotator).markKeyCooldown("gsk_key_1");
        verify(restClient, times(3)).post();
    }

    // =========================================================================
    // TIER 4: REAL-WORLD APPLICATION SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Tier 4 - R5.1: Main model regional outage: automatic degradation to fallback model preserves assistant availability")
    void testRegionalOutage_DegradesSeamlesslyToFallbackModel() {
        when(keyRotator.getActiveKey()).thenReturn("gsk_active_prod_key");

        RestClientResponseException gateway502 = new RestClientResponseException(
                "Bad Gateway", HttpStatusCode.valueOf(502), "Provider Cloudflare 502", null, null, null
        );

        GroqLlmClient.GroqMessage fallbackMsg = new GroqLlmClient.GroqMessage("assistant", "Tour lặn biển vẫn hoạt động bình thường!");
        GroqLlmClient.GroqResponse groqResponse = new GroqLlmClient.GroqResponse(List.of(new GroqLlmClient.GroqResponse.Choice(fallbackMsg)));

        when(responseSpec.body(eq(GroqLlmClient.GroqResponse.class)))
                .thenThrow(gateway502)
                .thenReturn(groqResponse);

        AiMessage userMsg = new AiMessage();
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Tour lặn biển chiều nay có khởi hành không?");

        LlmResponse response = groqLlmClient.generateResponse(List.of(userMsg));

        assertNotNull(response);
        assertEquals("Tour lặn biển vẫn hoạt động bình thường!", response.getContent());
        verify(restClient, times(2)).post();
    }
}
