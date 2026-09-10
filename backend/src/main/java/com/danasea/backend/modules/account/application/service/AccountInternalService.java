package com.danasea.backend.modules.account.application.service;

import java.util.UUID;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaUserRepository;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaRefreshTokenRepository;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.UserJpaEntity;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.RefreshTokenJpaEntity;
import com.danasea.backend.modules.account.infrastructure.mapper.UserMapper;
import com.danasea.backend.modules.account.infrastructure.mapper.RefreshTokenMapper;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountInternalService implements AccountInternalApi {

    private final JpaUserRepository userRepository;
    private final JpaRefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final CacheManager cacheManager;

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    @Override
    @Cacheable(value = "usersByEmail", key = "#email?.trim()?.toLowerCase()", condition = "#email != null && !#email.isBlank()", unless = "#result == null")
    public Optional<User> findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(normalizeEmail(email)).map(userMapper::toDomain);
    }

    @Override
    @Cacheable(value = "usersById", key = "#id", condition = "#id != null", unless = "#result == null")
    public Optional<User> findUserById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return userRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "usersByEmail", key = "#user?.email?.trim()?.toLowerCase()", condition = "#user != null && #user.email != null"),
        @CacheEvict(value = "usersById", key = "#user?.id", condition = "#user != null && #user.id != null"),
        @CacheEvict(value = "usersByEmail", key = "#result?.email?.trim()?.toLowerCase()", condition = "#result != null && #result.email != null"),
        @CacheEvict(value = "usersById", key = "#result?.id", condition = "#result != null && #result.id != null")
    })
    public User saveUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getEmail() != null) {
            user.setEmail(normalizeEmail(user.getEmail()));
        }

        String previousEmail = null;
        if (user.getId() != null) {
            previousEmail = userRepository.findById(user.getId())
                    .map(UserJpaEntity::getEmail)
                    .map(this::normalizeEmail)
                    .orElse(null);
        }

        UserJpaEntity entity = userMapper.toEntity(user);
        UserJpaEntity saved = userRepository.saveAndFlush(entity);

        // If email was changed or previously cached under an old email, evict it immediately
        if (previousEmail != null && !previousEmail.equals(user.getEmail())) {
            if (cacheManager != null) {
                Cache cache = cacheManager.getCache("usersByEmail");
                if (cache != null) {
                    cache.evict(previousEmail);
                }
            }
        }

        return userMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public RefreshToken saveRefreshToken(RefreshToken token) {
        RefreshTokenJpaEntity entity = refreshTokenMapper.toEntity(token);
        RefreshTokenJpaEntity saved = refreshTokenRepository.save(entity);
        return refreshTokenMapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findRefreshTokenByHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHash(tokenHash)
                .map(refreshTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeRefreshTokenFamily(UUID familyId) {
        List<RefreshTokenJpaEntity> tokens = refreshTokenRepository.findAllByFamilyId(familyId);
        OffsetDateTime now = OffsetDateTime.now();
        tokens.forEach(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
            }
        });
        refreshTokenRepository.saveAll(tokens);
    }
}
