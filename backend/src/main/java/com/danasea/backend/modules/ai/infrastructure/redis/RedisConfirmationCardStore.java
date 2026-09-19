package com.danasea.backend.modules.ai.infrastructure.redis;

import com.danasea.backend.modules.ai.application.port.ConfirmationCardStorePort;
import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisConfirmationCardStore implements ConfirmationCardStorePort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "ai:confirmation:";
    private static final Duration TTL = Duration.ofMinutes(15); // TTL = hold TTL

    @Override
    public void save(ConfirmationCard card) {
        String key = KEY_PREFIX + card.getId();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(card), TTL);
        } catch (Exception e) {
            log.error("Failed to save confirmation card to Redis", e);
        }
    }

    @Override
    public Optional<ConfirmationCard> findById(String id) {
        String key = KEY_PREFIX + id;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return Optional.of(objectMapper.readValue(value.toString(), ConfirmationCard.class));
            }
        } catch (Exception e) {
            log.error("Failed to get confirmation card from Redis", e);
        }
        return Optional.empty();
    }

    @Override
    public void delete(String id) {
        String key = KEY_PREFIX + id;
        redisTemplate.delete(key);
    }
}
