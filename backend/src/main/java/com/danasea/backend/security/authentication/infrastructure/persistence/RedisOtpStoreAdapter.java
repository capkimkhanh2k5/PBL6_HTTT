package com.danasea.backend.security.authentication.infrastructure.persistence;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.danasea.backend.security.authentication.application.ports.OtpStorePort;
import com.danasea.backend.security.authentication.domain.models.OtpDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RedisOtpStoreAdapter implements OtpStorePort {

    private static final Logger log = LoggerFactory.getLogger(RedisOtpStoreAdapter.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "otp:verify-email:";

    public RedisOtpStoreAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public void save(String email, OtpDetails details, Duration ttl) {
        String key = KEY_PREFIX + email;
        try {
            String json = objectMapper.writeValueAsString(details);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.error("Error serializing OtpDetails", e);
            throw new RuntimeException("Could not save OTP", e);
        }
    }

    @Override
    public Optional<OtpDetails> findByEmail(String email) {
        String key = KEY_PREFIX + email;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            OtpDetails details = objectMapper.readValue(json, OtpDetails.class);
            return Optional.of(details);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing OtpDetails", e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String email) {
        String key = KEY_PREFIX + email;
        redisTemplate.delete(key);
    }
}
