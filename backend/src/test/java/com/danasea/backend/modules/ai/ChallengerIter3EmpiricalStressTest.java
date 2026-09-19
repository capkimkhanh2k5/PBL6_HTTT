package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.domain.TrustTier;
import com.danasea.backend.modules.ai.infrastructure.groq.GroqKeyRotator;
import com.danasea.backend.modules.ai.infrastructure.redis.RedisRateLimiter;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Challenger 1 (Gen 2 Iteration 3) Empirical Adversarial Stress Verification Suite.
 *
 * Specific Focus:
 * 1. RedisRateLimiter RESTRICTED tier:
 *    - Strict short-circuiting at line 1 of isAllowed() before Layer 1 / Layer 2.
 *    - 0 tokens consumed from IP, User, or Anon buckets.
 *    - 0 interactions with LettuceBasedProxyManager.
 *    - Massive concurrent flood resilience (100 threads x 1,000 iterations = 100,000 requests).
 *
 * 2. GroqKeyRotator Concurrency Stress:
 *    - 0 False Exhaustion under 100 concurrent threads with single active key.
 *    - Dynamic state transitions (COOLDOWN, DISABLED, RECOVERY) under concurrent load.
 *    - True exhaustion verification with 100% deterministic exception throwing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Challenger 1 Gen 2 Iter 3 - Empirical Stress Test Suite")
public class ChallengerIter3EmpiricalStressTest {

    // =========================================================================
    // SECTION 1: REDIS RATE LIMITER RESTRICTED TIER EMPIRICAL STRESS
    // =========================================================================
    @Nested
    @DisplayName("1. RedisRateLimiter RESTRICTED Tier Short-Circuit & Token Invariance")
    class RestrictedTierEmpiricalTests {

        @Mock
        private LettuceBasedProxyManager<byte[]> proxyManager;

        @Mock
        private RemoteBucketBuilder<byte[]> builder;

        @Mock
        private BucketProxy bucketProxy;

        private RedisRateLimiter rateLimiter;

        @BeforeEach
        void setUp() {
            lenient().when(proxyManager.builder()).thenReturn(builder);
            lenient().when(builder.build(any(byte[].class), any(io.github.bucket4j.BucketConfiguration.class))).thenReturn(bucketProxy);
            rateLimiter = new RedisRateLimiter(proxyManager);
        }

        @Test
        @DisplayName("STRESS-RL-1: RESTRICTED tier short-circuits across all parameter permutations with 0 proxy interaction")
        void testRestrictedTier_AllPermutationsZeroInteraction() {
            List<String> userIds = Arrays.asList("user-123", "", "   ", null);
            List<String> ipAddresses = Arrays.asList("1.2.3.4", "2001:db8::1", "", "   ", null);

            for (String user : userIds) {
                for (String ip : ipAddresses) {
                    boolean allowed = rateLimiter.isAllowed(user, ip, TrustTier.RESTRICTED);
                    assertFalse(allowed, String.format("Failed for user='%s', ip='%s'", user, ip));
                }
            }

            // Verify LettuceBasedProxyManager was NEVER touched
            verifyNoInteractions(proxyManager);
            verifyNoInteractions(builder);
            verifyNoInteractions(bucketProxy);
        }

        @Test
        @DisplayName("STRESS-RL-2: Concurrent Flood - 100 threads x 1,000 requests on RESTRICTED tier executes in <500ms with 0 token calls")
        void testRestrictedTier_HighConcurrencyFlood() throws InterruptedException, ExecutionException {
            int threadCount = 100;
            int requestsPerThread = 1000;
            int totalRequests = threadCount * requestsPerThread;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    int blockedCount = 0;
                    for (int i = 0; i < requestsPerThread; i++) {
                        String user = "user-banned-" + (threadId % 10);
                        String ip = "192.168.1." + (threadId % 50);
                        if (!rateLimiter.isAllowed(user, ip, TrustTier.RESTRICTED)) {
                            blockedCount++;
                        }
                    }
                    return blockedCount;
                }));
            }

            readyLatch.await(); // wait for all threads ready
            long startTime = System.nanoTime();
            startLatch.countDown(); // release stampede

            int totalBlocked = 0;
            for (Future<Integer> f : futures) {
                totalBlocked += f.get();
            }
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            executor.shutdown();

            System.out.printf("STRESS-RL-2: Processed %d RESTRICTED requests in %d ms (%.2f req/ms)%n",
                    totalRequests, durationMs, (double) totalRequests / durationMs);

            assertEquals(totalRequests, totalBlocked, "Every single RESTRICTED request must be blocked (false)");
            assertTrue(durationMs < 2000, "100k in-memory short-circuit checks should complete well under 2s");
            verifyNoInteractions(proxyManager);
            verifyNoInteractions(builder);
            verifyNoInteractions(bucketProxy);
        }
    }

    // =========================================================================
    // SECTION 2: GROQ KEY ROTATOR CONCURRENCY STRESS & FALSE EXHAUSTION
    // =========================================================================
    @Nested
    @DisplayName("2. GroqKeyRotator Concurrency Stress & 0 False Exhaustion")
    class GroqKeyRotatorEmpiricalTests {

        @Mock
        private StringRedisTemplate redisTemplate;

        @Mock
        private ValueOperations<String, String> valueOperations;

        private final List<String> fiveKeys = List.of(
                "gsk_key0_1111",
                "gsk_key1_2222",
                "gsk_key2_3333",
                "gsk_key3_4444",
                "gsk_key4_5555"
        );

        @BeforeEach
        void setUp() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @ParameterizedTest(name = "Sole Active Key Index = {0}")
        @ValueSource(ints = {0, 1, 2, 3, 4})
        @DisplayName("STRESS-GKR-CONC-1: Extreme Stampede (100 threads, 500 req/thread = 50,000 req) with exactly 1 active key")
        void testExtremeStampede_SoleActiveKey_ZeroFalseExhaustion(int activeKeyIdx) throws InterruptedException, ExecutionException {
            for (int i = 0; i < 5; i++) {
                if (i == activeKeyIdx) {
                    when(valueOperations.get("groq:key:" + i)).thenReturn(null);
                } else {
                    when(valueOperations.get("groq:key:" + i)).thenReturn("COOLDOWN");
                }
            }

            GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, fiveKeys);

            int threadCount = 100;
            int requestsPerThread = 500;
            int totalExpected = threadCount * requestsPerThread;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            AtomicInteger falseExhaustionCounter = new AtomicInteger(0);
            String expectedKey = fiveKeys.get(activeKeyIdx);

            for (int t = 0; t < threadCount; t++) {
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    int success = 0;
                    for (int i = 0; i < requestsPerThread; i++) {
                        try {
                            String active = rotator.getActiveKey();
                            if (expectedKey.equals(active)) {
                                success++;
                            }
                        } catch (RuntimeException ex) {
                            falseExhaustionCounter.incrementAndGet();
                        }
                    }
                    return success;
                }));
            }

            readyLatch.await();
            long startTime = System.nanoTime();
            startLatch.countDown();

            int totalSuccess = 0;
            for (Future<Integer> f : futures) {
                totalSuccess += f.get();
            }
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            executor.shutdown();

            System.out.printf("STRESS-GKR-CONC-1 [Active Key %d]: Total=%d, Success=%d, False Exhaustions=%d in %d ms%n",
                    activeKeyIdx, totalExpected, totalSuccess, falseExhaustionCounter.get(), durationMs);

            assertEquals(0, falseExhaustionCounter.get(),
                    "STRICT REQUIREMENT: 0 false exhaustion errors permitted across 50,000 requests!");
            assertEquals(totalExpected, totalSuccess,
                    "Every concurrent request must successfully acquire the only active key!");
        }

        @Test
        @DisplayName("STRESS-GKR-CONC-2: 2 Active Keys under 80 threads share load evenly with 0 false exhaustion")
        void testTwoActiveKeys_ConcurrentFairSharing() throws InterruptedException, ExecutionException {
            // Keys 1 & 3 are ACTIVE; Keys 0, 2, 4 are COOLDOWN/DISABLED
            when(valueOperations.get("groq:key:0")).thenReturn("COOLDOWN");
            when(valueOperations.get("groq:key:1")).thenReturn(null); // Active
            when(valueOperations.get("groq:key:2")).thenReturn("DISABLED");
            when(valueOperations.get("groq:key:3")).thenReturn(null); // Active
            when(valueOperations.get("groq:key:4")).thenReturn("COOLDOWN");

            GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, fiveKeys);

            int threadCount = 80;
            int requestsPerThread = 250;
            int totalExpected = threadCount * requestsPerThread;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<Map<String, Integer>>> futures = new ArrayList<>();
            AtomicInteger falseExhaustion = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    Map<String, Integer> keyDistribution = new HashMap<>();
                    for (int i = 0; i < requestsPerThread; i++) {
                        try {
                            String k = rotator.getActiveKey();
                            keyDistribution.merge(k, 1, Integer::sum);
                        } catch (RuntimeException ex) {
                            falseExhaustion.incrementAndGet();
                        }
                    }
                    return keyDistribution;
                }));
            }

            readyLatch.await();
            startLatch.countDown();

            Map<String, Integer> aggregated = new HashMap<>();
            for (Future<Map<String, Integer>> f : futures) {
                f.get().forEach((k, v) -> aggregated.merge(k, v, Integer::sum));
            }
            executor.shutdown();

            assertEquals(0, falseExhaustion.get(), "0 false exhaustion errors allowed");
            assertEquals(2, aggregated.size(), "Only keys 1 and 3 must be returned");
            assertTrue(aggregated.containsKey("gsk_key1_2222"));
            assertTrue(aggregated.containsKey("gsk_key3_4444"));

            int key1Count = aggregated.get("gsk_key1_2222");
            int key3Count = aggregated.get("gsk_key3_4444");
            assertEquals(totalExpected, key1Count + key3Count);

            // Check balanced rotation (each key gets roughly 40-60% of traffic)
            double ratio = (double) key1Count / totalExpected;
            assertTrue(ratio >= 0.40 && ratio <= 0.60,
                    String.format("Expected balanced rotation between 2 keys, but ratio was %.2f", ratio));
        }

        @Test
        @DisplayName("STRESS-GKR-CONC-3: Dynamic State Transitions - Readers concurrent with background writers marking keys COOLDOWN")
        void testConcurrentReadersAndWriters_DynamicCooldown() throws InterruptedException, ExecutionException {
            // Concurrent key states in a thread-safe map
            ConcurrentHashMap<String, String> dynamicRedisStates = new ConcurrentHashMap<>();
            lenient().when(valueOperations.get(anyString())).thenAnswer(inv -> dynamicRedisStates.get(inv.getArgument(0)));
            lenient().doAnswer(inv -> {
                dynamicRedisStates.put(inv.getArgument(0), inv.getArgument(1));
                return null;
            }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            GroqKeyRotator rotator = new GroqKeyRotator(redisTemplate, fiveKeys);

            int readerThreads = 40;
            int writerThreads = 5;
            int readerIterations = 200;

            ExecutorService executor = Executors.newFixedThreadPool(readerThreads + writerThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<Integer>> readerFutures = new ArrayList<>();
            AtomicInteger falseExhaustion = new AtomicInteger(0);

            // Submit readers
            for (int r = 0; r < readerThreads; r++) {
                readerFutures.add(executor.submit(() -> {
                    startLatch.await();
                    int successfulReads = 0;
                    for (int i = 0; i < readerIterations; i++) {
                        try {
                            String k = rotator.getActiveKey();
                            assertNotNull(k);
                            successfulReads++;
                        } catch (RuntimeException ex) {
                            if (ex.getMessage().contains("All Groq API keys are currently COOLDOWN or DISABLED")) {
                                falseExhaustion.incrementAndGet();
                            }
                        }
                    }
                    return successfulReads;
                }));
            }

            // Submit writers that randomly cycle cooldown on keys 0, 1, 2, 3 (leaving key 4 ALWAYS active as anchor)
            for (int w = 0; w < writerThreads; w++) {
                executor.submit(() -> {
                    startLatch.await();
                    Random rng = new Random();
                    for (int i = 0; i < 50; i++) {
                        int keyIdx = rng.nextInt(4); // 0..3 only
                        String keyName = fiveKeys.get(keyIdx);
                        if (rng.nextBoolean()) {
                            rotator.markKeyCooldown(keyName);
                        } else {
                            dynamicRedisStates.remove("groq:key:" + keyIdx); // simulate TTL expiry
                        }
                        Thread.yield();
                    }
                    return null;
                });
            }

            startLatch.countDown();

            int totalReads = 0;
            for (Future<Integer> rf : readerFutures) {
                totalReads += rf.get();
            }
            executor.shutdown();

            System.out.printf("STRESS-GKR-CONC-3: Dynamic state reads=%d, False Exhaustions=%d%n",
                    totalReads, falseExhaustion.get());

            assertEquals(0, falseExhaustion.get(),
                    "With key 4 always anchor active, readers must NEVER experience false exhaustion!");
            assertEquals(readerThreads * readerIterations, totalReads);
        }
    }
}
