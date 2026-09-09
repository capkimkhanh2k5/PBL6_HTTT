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

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AccountInternalService implements AccountInternalApi {

    private final JpaUserRepository userRepository;
    private final JpaRefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findUserById(UUID id) {
        return userRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User saveUser(User user) {
        UserJpaEntity entity = userMapper.toEntity(user);
        UserJpaEntity saved = userRepository.saveAndFlush(entity);
        return userMapper.toDomain(saved);
    }

    @Override
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
