package com.danasea.backend.modules.ai.infrastructure.redis;

import com.danasea.backend.modules.ai.application.port.ConfirmationCardStorePort;
import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RedisConfirmationCardStore implements ConfirmationCardStorePort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, String> inMemoryFallback = new ConcurrentHashMap<>();
    private static final String KEY_PREFIX = "ai:confirmation:";
    private static final Duration TTL = Duration.ofMinutes(15); // TTL = hold TTL

    @Autowired
    public RedisConfirmationCardStore(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(ConfirmationCard card) {
        String key = KEY_PREFIX + card.getId();
        try {
            String json = objectMapper.writeValueAsString(card);
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, json, TTL);
            } else {
                inMemoryFallback.put(key, json);
            }
        } catch (Exception e) {
            log.error("Failed to save confirmation card", e);
        }
    }

    @Override
    public Optional<ConfirmationCard> findById(String id) {
        String key = KEY_PREFIX + id;
        try {
            String json;
            if (redisTemplate != null) {
                json = redisTemplate.opsForValue().get(key);
            } else {
                json = inMemoryFallback.get(key);
            }
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, ConfirmationCard.class));
            }
        } catch (Exception e) {
            log.error("Failed to get confirmation card", e);
        }
        return Optional.empty();
    }

    @Override
    public void delete(String id) {
        String key = KEY_PREFIX + id;
        if (redisTemplate != null) {
            redisTemplate.delete(key);
        } else {
            inMemoryFallback.remove(key);
        }
    }
}
