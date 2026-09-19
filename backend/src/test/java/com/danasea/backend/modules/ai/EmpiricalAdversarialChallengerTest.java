package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.application.port.ConfirmationCardStorePort;
import com.danasea.backend.modules.ai.application.port.LlmClientPort;
import com.danasea.backend.modules.ai.application.port.ModerationPort;
import com.danasea.backend.modules.ai.application.tool.GetPolicyTool;
import com.danasea.backend.modules.ai.application.tool.GetSafetyAlertTool;
import com.danasea.backend.modules.ai.application.tool.GetWeatherForecastTool;
import com.danasea.backend.modules.ai.application.tool.ToolExecutor;
import com.danasea.backend.modules.ai.application.usecase.ChatUseCase;
import com.danasea.backend.modules.ai.application.usecase.ConfirmBookingUseCase;
import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import com.danasea.backend.modules.ai.domain.models.LlmResponse;
import com.danasea.backend.modules.ai.domain.models.ToolCall;
import com.danasea.backend.modules.ai.domain.services.AIToolRegistry;
import com.danasea.backend.modules.ai.domain.services.ChatHistoryService;
import com.danasea.backend.modules.ai.domain.services.SystemPromptBuilder;
import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.application.usecases.GetPublicServiceDetailUseCase;
import com.danasea.backend.modules.systemconfig.infrastructure.persistence.entities.SystemConfigJpaEntity;
import com.danasea.backend.modules.systemconfig.infrastructure.persistence.repositories.JpaSystemConfigRepository;
import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.usecases.GetWeatherInfoUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Empirical Adversarial Challenger Test Suite (Generation 2)
 *
 * Designed to stress-test and empirically verify:
 * 1. Policy keyword rejection when get_policy is not called in the same turn.
 * 2. Booking confirmation 2-retry Redis limits, price change draft card generation (reason=PRICE_CHANGED),
 *    slot exhaustion handling, and Redis resilience.
 * 3. Weather and Safety tool caching behaviors (Redis TTL 10m, Cache Hit/Miss, fallback on Redis failure).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Empirical Adversarial Challenger Verification Suite")
public class EmpiricalAdversarialChallengerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================================
    // SECTION 1: POLICY KEYWORD REJECTION ADVERSARIAL TESTS
    // =========================================================================
    @Nested
    @DisplayName("1. Policy Keyword Rejection & Enforcement Tests")
    class PolicyKeywordRejectionTests {

        @Mock
        private LlmClientPort llmClientPort;

        @Mock
        private ModerationPort moderationPort;

        @Mock
        private ChatHistoryService chatHistoryService;

        @Mock
        private ToolExecutor searchTool;

        @Mock
        private ToolExecutor policyTool;

        private ChatUseCase chatUseCase;
        private final UUID conversationId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
            lenient().when(searchTool.getName()).thenReturn("search_service");
            lenient().when(policyTool.getName()).thenReturn("get_policy");
            lenient().when(moderationPort.isSafe(any())).thenReturn(true);

            chatUseCase = new ChatUseCase(
                    llmClientPort,
                    moderationPort,
                    chatHistoryService,
                    List.of(searchTool, policyTool),
                    objectMapper
            );
        }

        @Test
        @DisplayName("Adv-1A: Rejection when LLM outputs 'hoàn tiền' keyword WITHOUT calling get_policy")
        void testPolicyRejection_HoanTienWithoutToolCall() {
            LlmResponse hallucinatedResp = new LlmResponse();
            hallucinatedResp.setContent("Chúng tôi sẽ hoàn tiền 100% nếu bạn hủy trước 3 ngày.");
            hallucinatedResp.setToolCalls(null);

            when(llmClientPort.generateResponse(any())).thenReturn(hallucinatedResp);

            LlmResponse result = chatUseCase.processMessage(conversationId, "Hủy tour có được hoàn tiền không?");

            assertNotNull(result);
            assertEquals("Tôi không thể cung cấp thông tin về chính sách hoàn hủy khi chưa tra cứu hệ thống chính thức. Vui lòng yêu cầu kiểm tra chính sách cụ thể để tôi hỗ trợ.",
                    result.getContent(), "Hallucinated refund policy must be intercepted and rejected");
            verify(policyTool, never()).execute(anyString());
        }

        @Test
        @DisplayName("Adv-1B: Rejection when LLM outputs 'cancellation policy' in English WITHOUT calling get_policy")
        void testPolicyRejection_EnglishCancellationPolicyWithoutToolCall() {
            LlmResponse hallucinatedResp = new LlmResponse();
            hallucinatedResp.setContent("Our cancellation policy allows full refund within 48 hours.");
            hallucinatedResp.setToolCalls(null);

            when(llmClientPort.generateResponse(any())).thenReturn(hallucinatedResp);

            LlmResponse result = chatUseCase.processMessage(conversationId, "What is the cancellation policy?");

            assertNotNull(result);
            assertTrue(result.getContent().contains("Tôi không thể cung cấp thông tin về chính sách hoàn hủy khi chưa tra cứu"),
                    "English cancellation policy without get_policy must be intercepted");
        }

        @Test
        @DisplayName("Adv-1C: Case-insensitive rejection: uppercase 'CHÍNH SÁCH HỦY' intercepted")
        void testPolicyRejection_CaseInsensitiveUppercase() {
            LlmResponse hallucinatedResp = new LlmResponse();
            hallucinatedResp.setContent("CHÍNH SÁCH HỦY TOUR CỦA CHÚNG TÔI LÀ MIỄN PHÍ.");
            hallucinatedResp.setToolCalls(null);

            when(llmClientPort.generateResponse(any())).thenReturn(hallucinatedResp);

            LlmResponse result = chatUseCase.processMessage(conversationId, "Chính sách hủy thế nào?");

            assertEquals("Tôi không thể cung cấp thông tin về chính sách hoàn hủy khi chưa tra cứu hệ thống chính thức. Vui lòng yêu cầu kiểm tra chính sách cụ thể để tôi hỗ trợ.",
                    result.getContent());
        }

        @Test
        @DisplayName("Adv-1D: Acceptance when get_policy IS called in the same turn before outputting policy text")
        void testPolicyAllowed_WhenGetPolicyCalledInSameTurn() {
            // Step 1: LLM calls get_policy
            LlmResponse toolCallResp = new LlmResponse();
            ToolCall tc = new ToolCall("tc-pol-valid", "get_policy", "{\"policy_type\":\"CANCELLATION\"}");
            toolCallResp.setToolCalls(List.of(tc));

            // Step 2: LLM outputs response with policy keywords based on tool result
            LlmResponse finalResp = new LlmResponse();
            finalResp.setContent("Theo quy định chính sách hủy: hoàn tiền 100% khi hủy trước 24h.");

            when(llmClientPort.generateResponse(any()))
                    .thenReturn(toolCallResp)
                    .thenReturn(finalResp);
            when(policyTool.execute(anyString())).thenReturn("{\"policy\":\"Hoàn 100% trước 24h\"}");

            LlmResponse result = chatUseCase.processMessage(conversationId, "Xem chính sách hủy giúp tôi");

            assertEquals("Theo quy định chính sách hủy: hoàn tiền 100% khi hủy trước 24h.", result.getContent(),
                    "Legitimate response backed by get_policy must NOT be rejected");
            verify(policyTool, times(1)).execute(anyString());
        }

        @Test
        @DisplayName("Adv-1E: Rejection when LLM calls a DIFFERENT tool (search_service) but still outputs policy keywords")
        void testPolicyRejection_WhenDifferentToolCalledWithoutGetPolicy() {
            // LLM calls search_service, NOT get_policy
            LlmResponse toolCallResp = new LlmResponse();
            ToolCall tc = new ToolCall("tc-search-1", "search_service", "{\"keyword\":\"lặn\"}");
            toolCallResp.setToolCalls(List.of(tc));

            // In next step, LLM tries to hallucinate refund policy
            LlmResponse finalResp = new LlmResponse();
            finalResp.setContent("Đã tìm thấy tour. Lưu ý: chính sách hủy là hoàn tiền 50%.");

            when(llmClientPort.generateResponse(any()))
                    .thenReturn(toolCallResp)
                    .thenReturn(finalResp);
            when(searchTool.execute(anyString())).thenReturn("{\"results\":[]}");

            LlmResponse result = chatUseCase.processMessage(conversationId, "Tìm tour và cho biết chính sách hoàn tiền");

            assertEquals("Tôi không thể cung cấp thông tin về chính sách hoàn hủy khi chưa tra cứu hệ thống chính thức. Vui lòng yêu cầu kiểm tra chính sách cụ thể để tôi hỗ trợ.",
                    result.getContent(), "Calling another tool does not bypass the requirement for get_policy");
        }

        @Test
        @DisplayName("Adv-1F: Legacy alias 'get_cancellation_policy' also grants policy authorization")
        void testPolicyAllowed_WhenLegacyAliasCalled() {
            LlmResponse toolCallResp = new LlmResponse();
            ToolCall tc = new ToolCall("tc-legacy-pol", "get_cancellation_policy", "{}");
            toolCallResp.setToolCalls(List.of(tc));

            LlmResponse finalResp = new LlmResponse();
            finalResp.setContent("Chính sách hủy tour theo hệ thống là hoàn tiền trước 24h.");

            when(llmClientPort.generateResponse(any()))
                    .thenReturn(toolCallResp)
                    .thenReturn(finalResp);

            LlmResponse result = chatUseCase.processMessage(conversationId, "Xem quy định hoàn");

            assertEquals("Chính sách hủy tour theo hệ thống là hoàn tiền trước 24h.", result.getContent());
        }
    }

    // =========================================================================
    // SECTION 2: BOOKING RE-VALIDATION & REDIS RETRY LIMIT ADVERSARIAL TESTS
    // =========================================================================
    @Nested
    @DisplayName("2. Booking Re-validation, Redis Retries & Draft Card Reason Tests")
    class BookingRevalidationAdversarialTests {

        @Mock
        private ConfirmationCardStorePort cardStorePort;

        @Mock
        private GetPublicServiceDetailUseCase getServiceDetailUseCase;

        @Mock
        private StringRedisTemplate redisTemplate;

        @Mock
        private ValueOperations<String, String> valueOperations;

        private ConfirmBookingUseCase confirmBookingUseCase;
        private final UUID userId = UUID.randomUUID();
        private final UUID serviceId = UUID.randomUUID();
        private final UUID conversationId = UUID.randomUUID();
        private final String sessionId = "session-adv-456";

        private ConfirmationCard pendingCard;

        @BeforeEach
        void setUp() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            confirmBookingUseCase = new ConfirmBookingUseCase(
                    cardStorePort,
                    getServiceDetailUseCase,
                    redisTemplate
            );

            pendingCard = ConfirmationCard.builder()
                    .id("card-adv-1")
                    .conversationId(conversationId)
                    .serviceId(serviceId)
                    .price(new BigDecimal("500000.00"))
                    .date("2026-09-20T08:00:00")
                    .quantity(2)
                    .status("PENDING")
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("Adv-2A: Price change creates new Draft Card with reason=PRICE_CHANGED and writes Redis retry=1")
        void testPriceChange_CreatesDraftCardWithPriceChangedReasonAndRedisTracked() {
            ServiceDetailResult newDetail = ServiceDetailResult.builder()
                    .price(new BigDecimal("600000.00"))
                    .availableSlots(List.of("2026-09-20T08:00:00"))
                    .build();

            when(cardStorePort.findById("card-adv-1")).thenReturn(Optional.of(pendingCard));
            when(getServiceDetailUseCase.execute(serviceId, userId, sessionId)).thenReturn(newDetail);
            when(valueOperations.get("ai:booking:retries:" + conversationId)).thenReturn(null);

            Map<String, Object> response = confirmBookingUseCase.execute("card-adv-1", userId, sessionId);

            assertEquals("alternative_needed", response.get("status"));
            assertEquals("PRICE_CHANGED", response.get("reason"), "Response must carry reason PRICE_CHANGED");

            ConfirmationCard newCard = (ConfirmationCard) response.get("newCard");
            assertNotNull(newCard);
            assertEquals("PRICE_CHANGED", newCard.getReason(), "New Draft Card must carry reason PRICE_CHANGED");
            assertEquals(new BigDecimal("600000.00"), newCard.getPrice());
            assertEquals(1, newCard.getRetryCount());

            // Verify Redis tracking: key "ai:booking:retries:{convId}" set to "1" with 15 min TTL
            verify(valueOperations, times(1)).set(
                    eq("ai:booking:retries:" + conversationId),
                    eq("1"),
                    eq(Duration.ofMinutes(15))
            );
        }

        @Test
        @DisplayName("Adv-2B: Attempt 2 (retry 1 -> 2) writes '2' to Redis with 15m TTL")
        void testRetryCount1To2_WritesRedisWithTTL() {
            pendingCard.setRetryCount(1);
            when(cardStorePort.findById("card-adv-1")).thenReturn(Optional.of(pendingCard));
            when(valueOperations.get("ai:booking:retries:" + conversationId)).thenReturn("1");

            ServiceDetailResult newDetail = ServiceDetailResult.builder()
                    .price(new BigDecimal("650000.00"))
                    .availableSlots(List.of("2026-09-20T08:00:00"))
                    .build();
            when(getServiceDetailUseCase.execute(serviceId, userId, sessionId)).thenReturn(newDetail);

            Map<String, Object> response = confirmBookingUseCase.execute("card-adv-1", userId, sessionId);

            assertEquals("alternative_needed", response.get("status"));
            ConfirmationCard newCard = (ConfirmationCard) response.get("newCard");
            assertEquals(2, newCard.getRetryCount());

            // Verify Redis set to 2 with 15 min TTL
            verify(valueOperations, times(1)).set(
                    eq("ai:booking:retries:" + conversationId),
                    eq("2"),
                    eq(Duration.ofMinutes(15))
            );
        }

        @Test
        @DisplayName("Adv-2C: Attempt 3 (retry 2 -> error) rejects with max retries exceeded, marks card CANCELLED")
        void testAttempt3_RejectsWithError_CancelsCard_NoNewCardSaved() {
            when(cardStorePort.findById("card-adv-1")).thenReturn(Optional.of(pendingCard));
            when(valueOperations.get("ai:booking:retries:" + conversationId)).thenReturn("2");

            ServiceDetailResult newDetail = ServiceDetailResult.builder()
                    .price(new BigDecimal("700000.00"))
                    .availableSlots(List.of("2026-09-20T08:00:00"))
                    .build();
            when(getServiceDetailUseCase.execute(serviceId, userId, sessionId)).thenReturn(newDetail);

            Map<String, Object> response = confirmBookingUseCase.execute("card-adv-1", userId, sessionId);

            assertEquals("error", response.get("status"));
            assertTrue(response.get("message").toString().contains("Service details have changed too many times"));
            assertEquals("CANCELLED", pendingCard.getStatus());

            // Only the old card marked CANCELLED is saved; no new Draft Card created
            verify(cardStorePort, times(1)).save(pendingCard);
            verify(valueOperations, never()).set(eq("ai:booking:retries:" + conversationId), eq("3"), any(Duration.class));
        }

        @Test
        @DisplayName("Adv-2D: Complete slot exhaustion (empty availableSlots) returns out_of_stock")
        void testSlotExhaustion_ReturnsOutOfStock() {
            when(cardStorePort.findById("card-adv-1")).thenReturn(Optional.of(pendingCard));

            ServiceDetailResult emptySlotsDetail = ServiceDetailResult.builder()
                    .price(new BigDecimal("500000.00"))
                    .availableSlots(Collections.emptyList())
                    .build();
            when(getServiceDetailUseCase.execute(serviceId, userId, sessionId)).thenReturn(emptySlotsDetail);

            Map<String, Object> response = confirmBookingUseCase.execute("card-adv-1", userId, sessionId);

            assertEquals("out_of_stock", response.get("status"));
            assertTrue(response.get("message").toString().contains("Khung giờ hoặc dịch vụ đã hết chỗ"));
            assertEquals("CANCELLED", pendingCard.getStatus());
            verify(cardStorePort, times(1)).save(pendingCard);
        }

        @Test
        @DisplayName("Adv-2E: Slot changed but price unchanged produces Draft Card with reason=SLOT_UNAVAILABLE")
        void testSlotChangedPriceUnchanged_ProducesSlotUnavailableReason() {
            when(cardStorePort.findById("card-adv-1")).thenReturn(Optional.of(pendingCard));

            ServiceDetailResult slotChangeDetail = ServiceDetailResult.builder()
                    .price(new BigDecimal("500000.00")) // Price is identical
                    .availableSlots(List.of("2026-09-20T14:00:00")) // Requested was 08:00:00
                    .build();
            when(getServiceDetailUseCase.execute(serviceId, userId, sessionId)).thenReturn(slotChangeDetail);

            Map<String, Object> response = confirmBookingUseCase.execute("card-adv-1", userId, sessionId);

            assertEquals("alternative_needed", response.get("status"));
            assertEquals("SLOT_UNAVAILABLE", response.get("reason"));
            ConfirmationCard newCard = (ConfirmationCard) response.get("newCard");
            assertNotNull(newCard);
            assertEquals("SLOT_UNAVAILABLE", newCard.getReason());
            assertEquals("2026-09-20T14:00:00", newCard.getDate());
        }

        @Test
        @DisplayName("Adv-2F: Redis connection failure resilience: degrades gracefully to in-memory card retry count")
        void testRedisDown_DegradesGracefullyToCardRetryCount() {
            when(cardStorePort.findById("card-adv-1")).thenReturn(Optional.of(pendingCard));
            when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("Redis offline"));
            doThrow(new RedisConnectionFailureException("Redis offline")).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            ServiceDetailResult newDetail = ServiceDetailResult.builder()
                    .price(new BigDecimal("550000.00"))
                    .availableSlots(List.of("2026-09-20T08:00:00"))
                    .build();
            when(getServiceDetailUseCase.execute(serviceId, userId, sessionId)).thenReturn(newDetail);

            // Should NOT throw exception, but gracefully proceed with card retry count
            Map<String, Object> response = assertDoesNotThrow(() ->
                    confirmBookingUseCase.execute("card-adv-1", userId, sessionId));

            assertEquals("alternative_needed", response.get("status"));
            ConfirmationCard newCard = (ConfirmationCard) response.get("newCard");
            assertEquals(1, newCard.getRetryCount());
        }
    }

    // =========================================================================
    // SECTION 3: WEATHER & SAFETY TOOL CACHING BEHAVIORS
    // =========================================================================
    @Nested
    @DisplayName("3. Weather & Safety Tool Caching & TTL Tests")
    class WeatherAndSafetyToolCachingTests {

        @Mock
        private GetWeatherInfoUseCase weatherInfoUseCase;

        @Mock
        private StringRedisTemplate redisTemplate;

        @Mock
        private ValueOperations<String, String> valueOperations;

        @BeforeEach
        void setUp() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @Test
        @DisplayName("Adv-3A: GetWeatherForecastTool Cache Miss queries use case and writes to Redis with 10m TTL")
        void testWeatherForecast_CacheMiss_CachesWith10mTTL() {
            GetWeatherForecastTool tool = new GetWeatherForecastTool(weatherInfoUseCase, redisTemplate, objectMapper);

            String location = "Sơn Trà";
            String date = "2026-09-21";
            String expectedCacheKey = "ai:weather:forecast:" + location + ":" + date;

            when(valueOperations.get(expectedCacheKey)).thenReturn(null); // Cache miss

            WeatherInfoDto weatherInfo = new WeatherInfoDto();
            WeatherInfoDto.WeatherData weatherData = new WeatherInfoDto.WeatherData();
            weatherData.setTemperature(30.0);
            weatherData.setWindSpeed(15.0);
            weatherInfo.setWeather(weatherData);

            WeatherInfoDto.MarineData marineData = new WeatherInfoDto.MarineData();
            marineData.setWaveHeight(0.8);
            weatherInfo.setMarine(marineData);

            when(weatherInfoUseCase.execute()).thenReturn(weatherInfo);

            String result = tool.execute(String.format("{\"location\":\"%s\",\"date\":\"%s\"}", location, date));

            assertNotNull(result);
            assertTrue(result.contains("30.0"));
            assertTrue(result.contains("0.8"));

            // Verify cache write with 10 minute TTL
            verify(valueOperations, times(1)).set(
                    eq(expectedCacheKey),
                    anyString(),
                    eq(Duration.ofMinutes(10))
            );
            verify(weatherInfoUseCase, times(1)).execute();
        }

        @Test
        @DisplayName("Adv-3B: GetWeatherForecastTool Cache Hit returns cached data without invoking use case")
        void testWeatherForecast_CacheHit_ReturnsImmediately() {
            GetWeatherForecastTool tool = new GetWeatherForecastTool(weatherInfoUseCase, redisTemplate, objectMapper);

            String location = "Mỹ Khê";
            String date = "2026-09-22";
            String expectedCacheKey = "ai:weather:forecast:" + location + ":" + date;
            String cachedJson = "{\"location\":\"Mỹ Khê\",\"temperature\":29.0,\"condition\":\"Sunny\"}";

            when(valueOperations.get(expectedCacheKey)).thenReturn(cachedJson);

            String result = tool.execute(String.format("{\"location\":\"%s\",\"date\":\"%s\"}", location, date));

            assertEquals(cachedJson, result);
            verify(weatherInfoUseCase, never()).execute();
        }

        @Test
        @DisplayName("Adv-3C: GetSafetyAlertTool Cache Miss queries use case, evaluates wave risk, and caches with 10m TTL")
        void testSafetyAlert_CacheMiss_EvaluatesHighWaveAndCaches() {
            GetSafetyAlertTool tool = new GetSafetyAlertTool(weatherInfoUseCase, redisTemplate, objectMapper);

            String location = "Bán đảo Sơn Trà";
            String expectedCacheKey = "ai:weather:safety:" + location;

            when(valueOperations.get(expectedCacheKey)).thenReturn(null); // Cache miss

            WeatherInfoDto weatherInfo = new WeatherInfoDto();
            WeatherInfoDto.MarineData marineData = new WeatherInfoDto.MarineData();
            marineData.setWaveHeight(2.5); // Sóng > 2.0m -> RED alert, isSafe = false
            weatherInfo.setMarine(marineData);

            when(weatherInfoUseCase.execute()).thenReturn(weatherInfo);

            String result = tool.execute(String.format("{\"location\":\"%s\"}", location));

            assertNotNull(result);
            assertTrue(result.contains("\"isSafe\":false"), "Wave height > 2.0m must flag isSafe=false");
            assertTrue(result.contains("\"alertLevel\":\"RED\""), "Wave height > 2.0m must set RED alert");

            verify(valueOperations, times(1)).set(
                    eq(expectedCacheKey),
                    anyString(),
                    eq(Duration.ofMinutes(10))
            );
        }

        @Test
        @DisplayName("Adv-3D: GetSafetyAlertTool Cache Hit returns cached alert without invoking use case")
        void testSafetyAlert_CacheHit_ReturnsImmediately() {
            GetSafetyAlertTool tool = new GetSafetyAlertTool(weatherInfoUseCase, redisTemplate, objectMapper);

            String location = "Bãi tắm Non Nước";
            String expectedCacheKey = "ai:weather:safety:" + location;
            String cachedAlert = "{\"location\":\"Bãi tắm Non Nước\",\"isSafe\":true,\"alertLevel\":\"GREEN\"}";

            when(valueOperations.get(expectedCacheKey)).thenReturn(cachedAlert);

            String result = tool.execute(String.format("{\"location\":\"%s\"}", location));

            assertEquals(cachedAlert, result);
            verify(weatherInfoUseCase, never()).execute();
        }

        @Test
        @DisplayName("Adv-3E: Weather and Safety tools gracefully handle Redis downtime")
        void testWeatherAndSafetyTools_RedisDowntimeResilience() {
            GetWeatherForecastTool weatherTool = new GetWeatherForecastTool(weatherInfoUseCase, redisTemplate, objectMapper);
            GetSafetyAlertTool safetyTool = new GetSafetyAlertTool(weatherInfoUseCase, redisTemplate, objectMapper);

            when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("Redis timeout"));
            doThrow(new RedisConnectionFailureException("Redis timeout")).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            assertDoesNotThrow(() -> weatherTool.execute("{\"location\":\"Đà Nẵng\",\"date\":\"2026-09-20\"}"));
            assertDoesNotThrow(() -> safetyTool.execute("{\"location\":\"Đà Nẵng\"}"));
        }
    }

    // =========================================================================
    // SECTION 4: TOOL REGISTRY & SYSTEM PROMPT INTEGRATION
    // =========================================================================
    @Nested
    @DisplayName("4. Tool Registry & Policy System Prompt Conformance")
    class ToolRegistryAndSystemPromptTests {

        private final AIToolRegistry registry = new AIToolRegistry();
        private final SystemPromptBuilder promptBuilder = new SystemPromptBuilder();

        @Test
        @DisplayName("Adv-4A: Tool registry defines get_policy, get_weather_forecast, get_safety_alert")
        void testRegistryContainsRequiredTools() {
            List<Map<String, Object>> tools = registry.getToolDefinitions();
            List<String> names = tools.stream().map(t -> (String) t.get("name")).toList();

            assertTrue(names.contains("get_policy"), "AIToolRegistry must declare get_policy");
            assertTrue(names.contains("get_weather_forecast"), "AIToolRegistry must declare get_weather_forecast");
            assertTrue(names.contains("get_safety_alert"), "AIToolRegistry must declare get_safety_alert");
            assertTrue(names.contains("search_services"), "AIToolRegistry must declare search_services");
            assertTrue(names.contains("get_service_detail"), "AIToolRegistry must declare get_service_detail");
            assertTrue(names.contains("request_booking_confirmation"), "AIToolRegistry must declare request_booking_confirmation");
        }

        @Test
        @DisplayName("Adv-4B: System prompt mandates calling get_policy tool and forbids memory guessing")
        void testSystemPromptMandatesGetPolicy() {
            String prompt = promptBuilder.buildBasePrompt();
            assertTrue(prompt.contains("get_policy(policy_type)"), "Prompt must explicitly mention get_policy(policy_type)");
            assertTrue(prompt.contains("DO NOT make promises or statements about refunds, cancellation policies, or safety regulations from memory"));
            assertTrue(prompt.contains("get_weather_forecast"));
            assertTrue(prompt.contains("get_safety_alert"));
        }
    }

    // =========================================================================
    // SECTION 5: GET POLICY DATABASE LOOKUP & REPOSITORY INTEGRATION
    // =========================================================================
    @Nested
    @DisplayName("5. GetPolicyTool Repository Fallback & Lookups")
    class GetPolicyToolRepositoryTests {

        @Mock
        private JpaSystemConfigRepository systemConfigRepository;

        @Test
        @DisplayName("Adv-5A: GetPolicyTool fetches raw text from system_configs table without LLM modification")
        void testGetPolicyTool_FetchesFromDatabase() {
            GetPolicyTool tool = new GetPolicyTool(systemConfigRepository, objectMapper);

            SystemConfigJpaEntity entity = new SystemConfigJpaEntity();
            entity.setKey("POLICY_CANCELLATION");
            entity.setValue("Chính sách hủy tour: Hoàn 100% trước 48 tiếng, hoàn 50% trước 24 tiếng.");

            when(systemConfigRepository.findByKey("POLICY_CANCELLATION")).thenReturn(Optional.of(entity));

            String result = tool.execute("{\"policy_type\":\"CANCELLATION\"}");

            assertNotNull(result);
            assertTrue(result.contains("Chính sách hủy tour: Hoàn 100% trước 48 tiếng"));
            verify(systemConfigRepository).findByKey("POLICY_CANCELLATION");
        }

        @Test
        @DisplayName("Adv-5B: GetPolicyTool fallback to default policies when not configured in database")
        void testGetPolicyTool_FallbackToDefaults() {
            GetPolicyTool tool = new GetPolicyTool(systemConfigRepository, objectMapper);

            when(systemConfigRepository.findByKey(anyString())).thenReturn(Optional.empty());

            String result = tool.execute("{\"policy_type\":\"REFUND\"}");

            assertNotNull(result);
            assertTrue(result.contains("Chính sách hoàn tiền: Hoàn 100% tiền cọc"));
        }
    }
}
