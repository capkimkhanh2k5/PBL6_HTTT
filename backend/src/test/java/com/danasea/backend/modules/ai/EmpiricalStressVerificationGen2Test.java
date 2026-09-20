package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.domain.TrustTier;
import com.danasea.backend.modules.ai.infrastructure.groq.GroqKeyRotator;
import com.danasea.backend.modules.ai.infrastructure.groq.GroqModerationClient;
import com.danasea.backend.modules.ai.infrastructure.redis.RedisRateLimiter;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Empirical Stress & Adversarial Verification Test Suite.
 * Authored by Challenger 1 (Gen 2 Iteration 2) to probe concurrency race conditions,
 * state overwrites, and rate limiter bypass vectors.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Empirical Challenger Gen 2 Iteration 2 Verification Suite")
public class EmpiricalStressVerificationGen2Test {

    // =========================================================================
    // 1. CONCURRENCY & STRESS TEST: GroqKeyRotator (0 False Exhaustion)
    // =========================================================================
    @Nested
    @DisplayName("1. GroqKeyRotator Extreme Concurrency Stress Tests")
    class GroqKeyRotatorConcurrencyTests {

        @Mock
        private StringRedisTemplate redisTemplate;

        @Mock
        private ValueOperations<String, String> valueOperations;

        private final List<String> keys = List.of(
                "gsk_key0_AAAA",
                "gsk_key1_BBBB",
                "gsk_key2_CCCC",
                "gsk_key3_DDDD",
                "gsk_key4_EEEE"
        );

        @BeforeEach
        void setUp() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @ParameterizedTest(name = "Active Key Index = {0}")
        @ValueSource(ints = {0, 1, 2, 3, 4})
        @DisplayName("STRESS-GKR-1: High Concurrency (50 threads, 200 iterations = 10,000 requests) with 4 COOLDOWN keys")
        void testHighConcurrency_ZeroFalseExhaustion(int activeKeyIndex) throws InterruptedException, ExecutionException {
            // Configure Redis mock: 4 keys are COOLDOWN, exactly 1 key is ACTIVE (null in Redis)
            for (int i = 0; i < 5; i++) {
                if (i == activeKeyIndex) {
                    when(valueOperations.get("groq:key:" + i)).thenReturn(null);
                } else {
                    when(valueOperations.get("groq:key:" + i)).thenReturn("COOLDOWN");
                }
            }

            GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, keys);

            int threadCount = 50;
            int iterationsPerThread = 200;
            int totalExpected = threadCount * iterationsPerThread;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            AtomicInteger falseExhaustionCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                futures.add(executor.submit(() -> {
                    latch.await();
                    int success = 0;
                    for (int i = 0; i < iterationsPerThread; i++) {
                        try {
                            String active = rotator.getActiveKey();
                            if (keys.get(activeKeyIndex).equals(active)) {
                                success++;
                            }
                        } catch (RuntimeException e) {
                            falseExhaustionCount.incrementAndGet();
                        }
                    }
                    return success;
                }));
            }

            // Fire all 50 threads simultaneously
            latch.countDown();

            int totalSuccess = 0;
            for (Future<Integer> f : futures) {
                totalSuccess += f.get();
            }
            executor.shutdown();

            System.out.printf("STRESS-GKR-1 [Active Key %d]: Total=%d, Success=%d, False Exhaustions=%d%n",
                    activeKeyIndex, totalExpected, totalSuccess, falseExhaustionCount.get());

            assertEquals(0, falseExhaustionCount.get(),
                    "There must be ZERO false exhaustion errors under 50-thread concurrent load!");
            assertEquals(totalExpected, totalSuccess,
                    "Every concurrent request must successfully acquire the single active key!");
        }

        @Test
        @DisplayName("STRESS-GKR-2: Mixed Disabled and Cooldown States with Concurrent Access")
        void testMixedDisabledAndCooldownStates() throws InterruptedException, ExecutionException {
            // Key 0: DISABLED, Key 1: COOLDOWN, Key 2: DISABLED, Key 3: COOLDOWN, Key 4: ACTIVE
            when(valueOperations.get("groq:key:0")).thenReturn("DISABLED");
            when(valueOperations.get("groq:key:1")).thenReturn("COOLDOWN");
            when(valueOperations.get("groq:key:2")).thenReturn("DISABLED");
            when(valueOperations.get("groq:key:3")).thenReturn("COOLDOWN");
            when(valueOperations.get("groq:key:4")).thenReturn(null); // ACTIVE

            GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, keys);

            int threadCount = 20;
            int iterations = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            AtomicInteger failures = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                futures.add(executor.submit(() -> {
                    latch.await();
                    int count = 0;
                    for (int i = 0; i < iterations; i++) {
                        try {
                            String k = rotator.getActiveKey();
                            if ("gsk_key4_EEEE".equals(k)) {
                                count++;
                            }
                        } catch (RuntimeException ex) {
                            failures.incrementAndGet();
                        }
                    }
                    return count;
                }));
            }

            latch.countDown();
            int success = 0;
            for (Future<Integer> f : futures) {
                success += f.get();
            }
            executor.shutdown();

            assertEquals(0, failures.get());
            assertEquals(threadCount * iterations, success);
        }

        @Test
        @DisplayName("STRESS-GKR-3: True Exhaustion - All 5 keys in COOLDOWN/DISABLED throws 100% reliably")
        void testTrueExhaustion_ConsistentlyThrows() {
            for (int i = 0; i < 5; i++) {
                when(valueOperations.get("groq:key:" + i)).thenReturn("COOLDOWN");
            }

            GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, keys);

            for (int i = 0; i < 50; i++) {
                RuntimeException ex = assertThrows(RuntimeException.class, rotator::getActiveKey);
                assertTrue(ex.getMessage().contains("All Groq API keys are currently COOLDOWN or DISABLED"));
            }
        }
    }

    // =========================================================================
    // 2. STATE OVERWRITE RESILIENCE: markKeyCooldown cannot overwrite DISABLED
    // =========================================================================
    @Nested
    @DisplayName("2. State Preservation & Overwrite Tests")
    class StateOverwriteTests {

        @Mock
        private StringRedisTemplate redisTemplate;

        @Mock
        private ValueOperations<String, String> valueOperations;

        private final List<String> keys = List.of(
                "gsk_key0_AAAA",
                "gsk_key1_BBBB",
                "gsk_key2_CCCC",
                "gsk_key3_DDDD",
                "gsk_key4_EEEE"
        );

        @BeforeEach
        void setUp() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @Test
        @DisplayName("STATE-1: Concurrent markKeyCooldown calls on a DISABLED key never write COOLDOWN")
        void testConcurrentCooldownCallsOnDisabledKey() throws InterruptedException {
            GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, keys);

            // Key 2 is DISABLED
            when(valueOperations.get("groq:key:2")).thenReturn("DISABLED");

            int threads = 10;
            int callsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(1);

            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        latch.await();
                        for (int i = 0; i < callsPerThread; i++) {
                            rotator.markKeyCooldown("gsk_key2_CCCC");
                        }
                    } catch (Exception ignored) {
                    }
                });
            }

            latch.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            // Verify that opsForValue().set("groq:key:2", "COOLDOWN", ...) was NEVER called
            verify(valueOperations, never()).set(eq("groq:key:2"), eq("COOLDOWN"), any(Duration.class));
        }

        @Test
        @DisplayName("STATE-2: markKeyDisabled overwrites COOLDOWN with 30-day DISABLED")
        void testMarkKeyDisabledOverwritesCooldown() {
            GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, keys);

            rotator.markKeyDisabled("gsk_key1_BBBB");

            // Verify that DISABLED is written with 30 days TTL
            verify(valueOperations, times(1)).set(
                    eq("groq:key:1"),
                    eq("DISABLED"),
                    eq(Duration.ofDays(30))
            );
        }

        @Test
        @DisplayName("STATE-3: Null, empty, and unknown keys handled safely without exceptions")
        void testNullAndUnknownKeysHandledSafely() {
            GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, keys);

            assertDoesNotThrow(() -> rotator.markKeyCooldown(null));
            assertDoesNotThrow(() -> rotator.markKeyCooldown(""));
            assertDoesNotThrow(() -> rotator.markKeyCooldown("   "));
            assertDoesNotThrow(() -> rotator.markKeyCooldown("non_existent_key"));

            assertDoesNotThrow(() -> rotator.markKeyDisabled(null));
            assertDoesNotThrow(() -> rotator.markKeyDisabled(""));
            assertDoesNotThrow(() -> rotator.markKeyDisabled("   "));
            assertDoesNotThrow(() -> rotator.markKeyDisabled("non_existent_key"));

            verifyNoInteractions(valueOperations);
        }
    }

    // =========================================================================
    // 3. RATE LIMITER ADVERSARIAL: Rejection of null/blank identifiers
    // =========================================================================
    @Nested
    @DisplayName("3. Rate Limiter Null/Blank Rejection Tests")
    class RateLimiterIdentifierRejectionTests {

        @Mock
        private LettuceBasedProxyManager<byte[]> proxyManager;

        @Mock
        private RemoteBucketBuilder<byte[]> builder;

        @Mock
        private BucketProxy bucket;

        private RedisRateLimiter rateLimiter;

        @BeforeEach
        void setUp() {
            lenient().when(proxyManager.builder()).thenReturn(builder);
            lenient().when(builder.build(any(), any(BucketConfiguration.class))).thenReturn(bucket);
            lenient().when(bucket.tryConsume(anyLong())).thenReturn(true);
            rateLimiter = new RedisRateLimiter(proxyManager);
        }

        @Test
        @DisplayName("RL-ADV-1: Both userId and ipAddress null returns false")
        void testBothNull_ReturnsFalse() {
            boolean allowed = rateLimiter.isAllowed(null, null, TrustTier.UNVERIFIED);
            assertFalse(allowed, "Must reject request when both userId and ipAddress are null");
            verifyNoInteractions(proxyManager);
        }

        @Test
        @DisplayName("RL-ADV-2: Both userId and ipAddress blank/spaces returns false")
        void testBothBlankSpaces_ReturnsFalse() {
            boolean allowed = rateLimiter.isAllowed("   ", "     ", TrustTier.UNVERIFIED);
            assertFalse(allowed, "Must reject request when both userId and ipAddress are blank spaces");
            verifyNoInteractions(proxyManager);
        }

        @Test
        @DisplayName("RL-ADV-3: Tab and newline whitespace strings return false")
        void testTabAndNewlineWhitespace_ReturnsFalse() {
            boolean allowed1 = rateLimiter.isAllowed("\t\n", "\r\n", TrustTier.UNVERIFIED);
            boolean allowed2 = rateLimiter.isAllowed(null, "\t", TrustTier.VERIFIED);
            boolean allowed3 = rateLimiter.isAllowed("   ", null, TrustTier.TRUSTED);

            assertFalse(allowed1, "Must reject tabs and newlines as blank");
            assertFalse(allowed2, "Must reject tab IP as blank");
            assertFalse(allowed3, "Must reject space user as blank");
            verifyNoInteractions(proxyManager);
        }

        @Test
        @DisplayName("RL-ADV-4: Blank userId with valid IP uses anonymous bucket (capacity 5)")
        void testBlankUserIdWithValidIp_UsesAnonymousBucket() {
            when(builder.build(any(), any(BucketConfiguration.class))).thenReturn(bucket);
            when(bucket.tryConsume(1)).thenReturn(true);

            ArgumentCaptor<byte[]> keyCaptor = ArgumentCaptor.forClass(byte[].class);
            ArgumentCaptor<BucketConfiguration> configCaptor = ArgumentCaptor.forClass(BucketConfiguration.class);

            boolean allowed = rateLimiter.isAllowed("   ", "10.0.0.1", TrustTier.TRUSTED);
            assertTrue(allowed);

            // Expect 2 builds: IP soft throttle (ai:ratelimit:ip:10.0.0.1) and anon bucket (ai:ratelimit:anon:10.0.0.1)
            verify(builder, times(2)).build(keyCaptor.capture(), configCaptor.capture());

            String ipKey = new String(keyCaptor.getAllValues().get(0));
            String anonKey = new String(keyCaptor.getAllValues().get(1));
            assertEquals("ai:ratelimit:ip:10.0.0.1", ipKey);
            assertEquals("ai:ratelimit:anon:10.0.0.1", anonKey);

            // Anonymous bucket capacity is 5
            assertEquals(5, configCaptor.getAllValues().get(1).getBandwidths()[0].getCapacity());
        }

        @Test
        @DisplayName("RL-ADV-5: Valid userId with blank IP uses user bucket directly")
        void testValidUserIdWithBlankIp_UsesUserBucket() {
            when(builder.build(any(), any(BucketConfiguration.class))).thenReturn(bucket);
            when(bucket.tryConsume(1)).thenReturn(true);

            ArgumentCaptor<byte[]> keyCaptor = ArgumentCaptor.forClass(byte[].class);
            ArgumentCaptor<BucketConfiguration> configCaptor = ArgumentCaptor.forClass(BucketConfiguration.class);

            boolean allowed = rateLimiter.isAllowed("user-999", "  ", TrustTier.VERIFIED);
            assertTrue(allowed);

            // Expect exactly 1 build: user bucket (ai:ratelimit:user:user-999) with capacity 20
            verify(builder, times(1)).build(keyCaptor.capture(), configCaptor.capture());
            assertEquals("ai:ratelimit:user:user-999", new String(keyCaptor.getValue()));
            assertEquals(20, configCaptor.getValue().getBandwidths()[0].getCapacity());
        }

        @Test
        @DisplayName("RL-ADV-6: RESTRICTED tier always returns false immediately")
        void testRestrictedTierAlwaysReturnsFalse() {
            assertFalse(rateLimiter.isAllowed("user-banned", "1.2.3.4", TrustTier.RESTRICTED));
            assertFalse(rateLimiter.isAllowed(null, "1.2.3.4", TrustTier.RESTRICTED));
            assertFalse(rateLimiter.isAllowed("user-banned", null, TrustTier.RESTRICTED));
        }
    }

    // =========================================================================
    // 4. MODERATION TRUNCATION & SURROGATE PAIRS
    // =========================================================================
    @Nested
    @DisplayName("4. Moderation Truncation & UTF-16 Surrogate Pair Integrity")
    class ModerationSurrogateTests {

        @Mock
        private RestTemplate restTemplate;

        private GroqModerationClient moderationClient;

        @BeforeEach
        void setUp() {
            moderationClient = new GroqModerationClient(restTemplate, null, "model", "url");
        }

        @Test
        @DisplayName("MOD-UTF16-1: High surrogate at boundary 1799 is rolled back to 1799, avoiding dangling surrogate")
        void testDanglingSurrogateProtection() {
            // Create a string where index 1799 is the high surrogate '\uD83D' and index 1800 is low surrogate '\uDE00'
            String prefix = "A".repeat(1799);
            String emoji = "\uD83D\uDE00"; // 😀
            String input = prefix + emoji; // total length 1801

            String truncated = moderationClient.truncateForModeration(input, 450);

            // Verify length is 1799
            assertEquals(1799, truncated.length());
            // Verify last char is 'A' (not a dangling high surrogate)
            assertEquals('A', truncated.charAt(truncated.length() - 1));
            assertFalse(Character.isHighSurrogate(truncated.charAt(truncated.length() - 1)));
        }

        @Test
        @DisplayName("MOD-UTF16-2: Complete emoji before boundary (at indices 1798-1799) is fully preserved")
        void testCompleteEmojiPreserved() {
            String prefix = "A".repeat(1798);
            String emoji = "\uD83D\uDE00"; // 😀
            String input = prefix + emoji; // total length 1800

            String truncated = moderationClient.truncateForModeration(input, 450);

            assertEquals(1800, truncated.length());
            assertTrue(truncated.endsWith("\uD83D\uDE00"));
        }
    }
}
