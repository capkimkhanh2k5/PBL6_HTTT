package com.danasea.backend.modules.account.application.api;

import java.util.UUID;

import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.account.domain.models.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface AccountInternalApi {
    Optional<User> findUserByEmail(String email);

    Optional<User> findUserById(UUID id);

    boolean existsByEmail(String email);

    User saveUser(User user);

    RefreshToken saveRefreshToken(RefreshToken token);

    Optional<RefreshToken> findRefreshTokenByHash(String tokenHash);

    void revokeRefreshTokenFamily(UUID familyId);

    void revokeAllRefreshTokensByUserId(UUID userId);
}
