package com.danasea.backend.modules.account.application.service;

<<<<<<< HEAD
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
=======
import java.util.UUID;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.account.infrastructure.mapper.RefreshTokenMapper;
import com.danasea.backend.modules.account.infrastructure.mapper.UserMapper;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.RefreshTokenJpaEntity;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.UserJpaEntity;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaRefreshTokenRepository;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaUserRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountInternalService implements AccountInternalApi {

    private final JpaUserRepository userRepository;
    private final JpaRefreshTokenRepository refreshTokenRepository;
    private final JpaAuditLogRepository auditLogRepository;
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

    @Override
    @Transactional
    public void revokeAllRefreshTokensByUserId(UUID userId) {
        List<RefreshTokenJpaEntity> tokens = refreshTokenRepository.findAllByUserId(userId);
        OffsetDateTime now = OffsetDateTime.now();
        boolean hasUnrevoked = false;
        for (RefreshTokenJpaEntity token : tokens) {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
                hasUnrevoked = true;
            }
        }
        if (hasUnrevoked) {
            refreshTokenRepository.saveAll(tokens);
        }
    }

    @Override
    @Transactional
    public void revokeAllTokensByUserId(UUID userId) {
        revokeAllRefreshTokensByUserId(userId);
    }

    @Override
    public Page<User> findUsers(Pageable pageable, Role role, Boolean isLocked, String search) {
        Specification<UserJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (isLocked != null) {
                predicates.add(cb.equal(root.get("isLocked"), isLocked));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                predicates.add(cb.or(emailMatch, nameMatch));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserJpaEntity> entities = userRepository.findAll(spec, pageable);
        return entities.map(userMapper::toDomain);
    }

    @Override
    public void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setActorUserId(actorUserId);
        entity.setAction(action);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setMetadata(metadata);
        auditLogRepository.save(entity);
    }
}
