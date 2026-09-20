package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.infrastructure.groq.GroqKeyRotator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Acceptance Criteria & 4-Tier Test Suite for GroqKeyRotator.
 *
 * <p>Requirements:
 * - R5: GroqKeyRotator loads exactly 5 keys from GROQ_API_KEYS and logs count at startup.
 * - R5: Round-robin rotation across active keys.
 * - R5: Skip COOLDOWN keys (after 429) and DISABLED keys (after 401).
 * - R5: Throw RuntimeException when all keys are exhausted.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GroqKeyRotator Acceptance Test Suite (Tiers 1-4)")
public class GroqKeyRotatorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private GroqKeyRotator keyRotator;

    private final List<String> fiveKeys = Arrays.asList(
            "gsk_key1_1234",
            "gsk_key2_5678",
            "gsk_key3_9012",
            "gsk_key4_3456",
            "gsk_key5_7890"
    );

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        keyRotator = new GroqKeyRotator(redisTemplate, fiveKeys);
    }

    // =========================================================================
    // TIER 1: FEATURE COVERAGE (>=5 test cases across R5 core requirements)
    // =========================================================================

    @Test
    @DisplayName("Tier 1 - F5.1: Exactly 5 keys loaded and active rotate in round-robin order")
    void testRoundRobin_AcrossAllFiveKeys() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertEquals("gsk_key1_1234", keyRotator.getActiveKey());
        assertEquals("gsk_key2_5678", keyRotator.getActiveKey());
        assertEquals("gsk_key3_9012", keyRotator.getActiveKey());
        assertEquals("gsk_key4_3456", keyRotator.getActiveKey());
        assertEquals("gsk_key5_7890", keyRotator.getActiveKey());
        // Loops back to key 1
        assertEquals("gsk_key1_1234", keyRotator.getActiveKey());
    }

    @Test
    @DisplayName("Tier 1 - F5.2: Skip COOLDOWN key after 429 rate limit")
    void testSkipCooldownKey() {
        // Key 0 is COOLDOWN, others active
        when(valueOperations.get("groq:key:0")).thenReturn("COOLDOWN");
        when(valueOperations.get("groq:key:1")).thenReturn(null);

        String activeKey = keyRotator.getActiveKey();
        assertEquals("gsk_key2_5678", activeKey, "Should skip COOLDOWN key 0 and return key 1");
    }

    @Test
    @DisplayName("Tier 1 - F5.3: Skip DISABLED key after 401 unauthorized")
    void testSkipDisabledKey() {
        // Key 0 is DISABLED, others active
        when(valueOperations.get("groq:key:0")).thenReturn("DISABLED");
        when(valueOperations.get("groq:key:1")).thenReturn(null);

        String activeKey = keyRotator.getActiveKey();
        assertEquals("gsk_key2_5678", activeKey, "Should skip DISABLED key 0 and return key 1");
    }

    @Test
    @DisplayName("Tier 1 - F5.4: Skip both COOLDOWN and DISABLED keys in rotation")
    void testSkipMultipleCooldownAndDisabledKeys() {
        when(valueOperations.get("groq:key:0")).thenReturn("COOLDOWN");
        when(valueOperations.get("groq:key:1")).thenReturn("DISABLED");
        when(valueOperations.get("groq:key:2")).thenReturn("COOLDOWN");
        when(valueOperations.get("groq:key:3")).thenReturn(null);

        assertEquals("gsk_key4_3456", keyRotator.getActiveKey());
    }

    @Test
    @DisplayName("Tier 1 - F5.5: Throw RuntimeException when all 5 keys are exhausted")
    void testExhaustedKeys_ThrowsRuntimeException() {
        when(valueOperations.get(anyString())).thenReturn("COOLDOWN");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            keyRotator.getActiveKey();
        });

        assertEquals("All Groq API keys are currently COOLDOWN or DISABLED", exception.getMessage());
    }

    // =========================================================================
    // TIER 2: BOUNDARY & CORNER CASES (>=5 test cases)
    // =========================================================================

    @Test
    @DisplayName("Tier 2 - B5.1: Only one active key remaining out of five keys")
    void testSingleActiveKeyRemaining_ReturnsSameKeyRepeatedly() {
        when(valueOperations.get("groq:key:0")).thenReturn("COOLDOWN");
        when(valueOperations.get("groq:key:1")).thenReturn("DISABLED");
        when(valueOperations.get("groq:key:2")).thenReturn("COOLDOWN");
        when(valueOperations.get("groq:key:3")).thenReturn("DISABLED");
        when(valueOperations.get("groq:key:4")).thenReturn(null); // only key 4 active

        assertEquals("gsk_key5_7890", keyRotator.getActiveKey());
        assertEquals("gsk_key5_7890", keyRotator.getActiveKey());
        assertEquals("gsk_key5_7890", keyRotator.getActiveKey());
    }

    @Test
    @DisplayName("Tier 2 - B5.2: Empty API keys list throws RuntimeException")
    void testEmptyKeysList_ThrowsRuntimeException() {
        GroqKeyRotator emptyRotator = new GroqKeyRotator(redisTemplate, Collections.emptyList());
        RuntimeException ex = assertThrows(RuntimeException.class, emptyRotator::getActiveKey);
        assertEquals("No Groq API keys configured", ex.getMessage());
    }

    @Test
    @DisplayName("Tier 2 - B5.3: Null API keys list throws RuntimeException")
    void testNullKeysList_ThrowsRuntimeException() {
        GroqKeyRotator nullRotator = new GroqKeyRotator(redisTemplate, (List<String>) null);
        RuntimeException ex = assertThrows(RuntimeException.class, nullRotator::getActiveKey);
        assertEquals("No Groq API keys configured", ex.getMessage());
    }

    @Test
    @DisplayName("Tier 2 - B5.4: markKeyCooldown sets Redis state to COOLDOWN with 1 minute duration")
    void testMarkKeyCooldown_SetsRedisKeyWithOneMinuteTtl() {
        keyRotator.markKeyCooldown("gsk_key2_5678");

        verify(valueOperations).set(eq("groq:key:1"), eq("COOLDOWN"), eq(Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("Tier 2 - B5.5: markKeyDisabled sets Redis state to DISABLED with 30 days duration")
    void testMarkKeyDisabled_SetsRedisKeyWithThirtyDaysTtl() {
        keyRotator.markKeyDisabled("gsk_key3_9012");

        verify(valueOperations).set(eq("groq:key:2"), eq("DISABLED"), eq(Duration.ofDays(30)));
    }

    @Test
    @DisplayName("Tier 2 - B5.6: Marking non-existent key cooldown or disabled does not interact with Redis")
    void testMarkUnknownKey_DoesNotThrowOrCallRedis() {
        keyRotator.markKeyCooldown("unknown_key_9999");
        keyRotator.markKeyDisabled("unknown_key_9999");

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE COMBINATIONS
    // =========================================================================

    @Test
    @DisplayName("Tier 3 - C5.1: Key cooldown recovery - key becomes available when Redis state expires (returns null)")
    void testCooldownRecovery_KeyBecomesActiveAfterTtlExpiration() {
        // First call: key 0 is COOLDOWN -> returns key 1
        when(valueOperations.get("groq:key:0")).thenReturn("COOLDOWN");
        when(valueOperations.get("groq:key:1")).thenReturn(null);
        assertEquals("gsk_key2_5678", keyRotator.getActiveKey());

        // Second round: key 0 TTL has expired (Redis returns null) -> key 0 becomes available again
        when(valueOperations.get("groq:key:0")).thenReturn(null);
        when(valueOperations.get("groq:key:2")).thenReturn(null);
        when(valueOperations.get("groq:key:3")).thenReturn(null);
        when(valueOperations.get("groq:key:4")).thenReturn(null);

        assertEquals("gsk_key3_9012", keyRotator.getActiveKey());
        assertEquals("gsk_key4_3456", keyRotator.getActiveKey());
        assertEquals("gsk_key5_7890", keyRotator.getActiveKey());
        assertEquals("gsk_key1_1234", keyRotator.getActiveKey(), "Recovered key 0 should be picked up");
    }

    // =========================================================================
    // TIER 4: REAL-WORLD SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Tier 4 - R5.1: Multi-threaded concurrent key retrieval rotates without race conditions")
    void testConcurrentKeyRotation_AtomicIndexAdvancesSafely() throws InterruptedException, ExecutionException {
        when(valueOperations.get(anyString())).thenReturn(null);

        int threadCount = 10;
        int requestsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<List<String>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                List<String> retrievedKeys = new ArrayList<>();
                for (int j = 0; j < requestsPerThread; j++) {
                    retrievedKeys.add(keyRotator.getActiveKey());
                }
                return retrievedKeys;
            }));
        }

        latch.countDown(); // start all threads simultaneously

        int totalKeysRetrieved = 0;
        for (Future<List<String>> future : futures) {
            List<String> keys = future.get();
            assertEquals(requestsPerThread, keys.size());
            totalKeysRetrieved += keys.size();
            for (String key : keys) {
                assertTrue(fiveKeys.contains(key));
            }
        }

        assertEquals(threadCount * requestsPerThread, totalKeysRetrieved);
        executor.shutdown();
    }

    @Test
    @DisplayName("Tier 2 - B5.7: Exactly 5 keys validation succeeds when 5 keys loaded")
    void testValidateExactlyFiveKeys_Success() {
        GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, fiveKeys);
        assertEquals(5, rotator.getKeyCount());
        assertEquals(5, rotator.getApiKeys().size());
    }

    @Test
    @DisplayName("Tier 2 - B5.8: Exactly 5 keys validation throws when fewer than 5 keys configured")
    void testValidateExactlyFiveKeys_FailsWhenFewerThanFive() {
        List<String> threeKeys = List.of("key1", "key2", "key3");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new GroqKeyRotator(redisTemplate, threeKeys);
        });
        assertTrue(ex.getMessage().contains("requires exactly 5 API keys, but found: 3"));
    }

    @Test
    @DisplayName("Tier 2 - B5.9: Exactly 5 keys validation throws when more than 5 keys configured")
    void testValidateExactlyFiveKeys_FailsWhenMoreThanFive() {
        List<String> sixKeys = List.of("k1", "k2", "k3", "k4", "k5", "k6");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new GroqKeyRotator(redisTemplate, sixKeys);
        });
        assertTrue(ex.getMessage().contains("requires exactly 5 API keys, but found: 6"));
    }

    @Test
    @DisplayName("Tier 2 - B5.10: Exactly 5 keys validation parses comma-separated keys with whitespace")
    void testValidateExactlyFiveKeys_ParsesCommaSeparatedAndTrimsWhitespace() {
        List<String> commaSeparated = List.of("k1, k2 , k3", "k4", " k5 ");
        GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, commaSeparated);
        assertEquals(5, rotator.getKeyCount());
        assertEquals(List.of("k1", "k2", "k3", "k4", "k5"), rotator.getApiKeys());
    }
}
