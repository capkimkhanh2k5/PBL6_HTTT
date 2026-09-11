package com.danasea.backend.modules.account.application.api;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;

public interface AccountInternalApi {
    Optional<User> findUserByEmail(String email);

    Optional<User> findUserById(UUID id);

    boolean existsByEmail(String email);

    User saveUser(User user);

    RefreshToken saveRefreshToken(RefreshToken token);

    Optional<RefreshToken> findRefreshTokenByHash(String tokenHash);

    void revokeRefreshTokenFamily(UUID familyId);

    void revokeAllTokensByUserId(UUID userId);

    void revokeAllRefreshTokensByUserId(UUID userId);

    Page<User> findUsers(Pageable pageable, Role role, Boolean isLocked, String search);

    void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);
}
