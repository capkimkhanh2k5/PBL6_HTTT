package com.danasea.backend.modules.ai.infrastructure.groq;

import com.danasea.backend.modules.ai.application.port.KeyRotatorPort;
import com.danasea.backend.modules.ai.infrastructure.groq.config.GroqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class GroqKeyRotator implements KeyRotatorPort {

    private static final String REDIS_KEY_PREFIX = "groq:key:";
    private static final String STATE_COOLDOWN = "COOLDOWN";
    private static final String STATE_DISABLED = "DISABLED";
    private static final int EXPECTED_KEY_COUNT = 5;

    private final StringRedisTemplate redisTemplate;
    private final List<String> apiKeys;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    @Autowired
    public GroqKeyRotator(StringRedisTemplate redisTemplate, GroqProperties groqProperties) {
        this(redisTemplate, groqProperties != null ? groqProperties.apiKeys() : List.of());
    }

    public GroqKeyRotator(StringRedisTemplate redisTemplate, List<String> apiKeys) {
        this.redisTemplate = redisTemplate;

        if (apiKeys == null || apiKeys.isEmpty()) {
            this.apiKeys = List.of();
            log.warn("GroqKeyRotator initialized with 0 Groq API keys");
            return;
        }

        List<String> cleaned = new ArrayList<>();
        for (String item : apiKeys) {
            if (item != null) {
                String[] parts = item.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        cleaned.add(trimmed);
                    }
                }
            }
        }

        if (cleaned.isEmpty()) {
            this.apiKeys = List.of();
            log.warn("GroqKeyRotator initialized with 0 Groq API keys");
            return;
        }

        if (cleaned.size() != EXPECTED_KEY_COUNT) {
            throw new IllegalStateException("GroqKeyRotator requires exactly " + EXPECTED_KEY_COUNT
                    + " API keys, but found: " + cleaned.size());
        }

        this.apiKeys = List.copyOf(cleaned);

        log.info("GroqKeyRotator initialized with {} Groq API keys", this.apiKeys.size());
        for (int i = 0; i < this.apiKeys.size(); i++) {
            String k = this.apiKeys.get(i);
            String masked = (k.length() >= 4) ? "..." + k.substring(k.length() - 4) : "****";
            log.info("Loaded Groq Key [{}]: {}", i, masked);
        }
    }

    @Override
    public String getActiveKey() {
        if (apiKeys == null || apiKeys.isEmpty()) {
            throw new RuntimeException("No Groq API keys configured");
        }

        int totalKeys = apiKeys.size();
        int startIdx = Math.floorMod(currentIndex.getAndIncrement(), totalKeys);

        for (int i = 0; i < totalKeys; i++) {
            int idx = (startIdx + i) % totalKeys;
            String key = apiKeys.get(idx);

            String redisKey = REDIS_KEY_PREFIX + idx;
            String state = null;
            try {
                if (redisTemplate != null && redisTemplate.opsForValue() != null) {
                    state = redisTemplate.opsForValue().get(redisKey);
                }
            } catch (Exception e) {
                log.warn("Failed to check Redis key state for index {}: {}", idx, e.getMessage());
            }

            if (state == null || (!STATE_COOLDOWN.equals(state) && !STATE_DISABLED.equals(state))) {
                currentIndex.set((idx + 1) % totalKeys);
                return key;
            }
        }

        throw new RuntimeException("All Groq API keys are currently COOLDOWN or DISABLED");
    }

    @Override
    public void markKeyCooldown(String key) {
        if (key == null) return;
        int idx = apiKeys.indexOf(key.trim());
        if (idx != -1) {
            String redisKey = REDIS_KEY_PREFIX + idx;
            try {
                if (redisTemplate != null && redisTemplate.opsForValue() != null) {
                    String currentState = redisTemplate.opsForValue().get(redisKey);
                    if (STATE_DISABLED.equals(currentState)) {
                        log.info("Skipping markKeyCooldown for Groq API key [{}] because it is already DISABLED", idx);
                        return;
                    }
                    redisTemplate.opsForValue().set(redisKey, STATE_COOLDOWN, Duration.ofMinutes(1));
                    log.warn("Marked Groq API key [{}] as COOLDOWN for 1 minute", idx);
                }
            } catch (Exception e) {
                log.error("Failed to mark Groq API key [{}] as COOLDOWN in Redis: {}", idx, e.getMessage());
            }
        }
    }

    @Override
    public void markKeyDisabled(String key) {
        if (key == null) return;
        int idx = apiKeys.indexOf(key.trim());
        if (idx != -1) {
            String redisKey = REDIS_KEY_PREFIX + idx;
            try {
                if (redisTemplate != null && redisTemplate.opsForValue() != null) {
                    redisTemplate.opsForValue().set(redisKey, STATE_DISABLED, Duration.ofDays(30));
                    log.error("Marked Groq API key [{}] as DISABLED for 30 days", idx);
                }
            } catch (Exception e) {
                log.error("Failed to mark Groq API key [{}] as DISABLED in Redis: {}", idx, e.getMessage());
            }
        }
    }

    public int getKeyCount() {
        return apiKeys.size();
    }

    public List<String> getApiKeys() {
        return apiKeys;
    }
}
