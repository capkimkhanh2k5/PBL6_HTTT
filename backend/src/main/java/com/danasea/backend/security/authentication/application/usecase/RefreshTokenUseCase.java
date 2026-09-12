package com.danasea.backend.security.authentication.application.usecase;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.security.authentication.application.port.TokenProvider;
import com.danasea.backend.security.authentication.domain.exception.InvalidCredentialsException;
import com.danasea.backend.security.authentication.application.result.LoginResult;
import com.danasea.backend.security.authentication.domain.model.Authentication;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;

import lombok.RequiredArgsConstructor;

import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;

@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final AccountInternalApi accountApi;
    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResult execute(String rawRefreshToken) {
        String tokenHash = HashUtils.sha256(rawRefreshToken);

        RefreshToken refreshToken = accountApi.findRefreshTokenByHash(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        // If the token is already revoked, it means someone is trying to reuse an old
        // token.
        // This is a sign of a compromised token chain. Revoke the entire family.
        if (refreshToken.getRevokedAt() != null) {
            accountApi.revokeRefreshTokenFamily(refreshToken.getFamilyId());
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidCredentialsException("Refresh token expired");
        }

        User user = accountApi.findUserById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new InvalidCredentialsException("User is locked or inactive");
        }

        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new InvalidCredentialsException("Email not verified");
        }

        Authentication auth = new Authentication(user.getId(), user.getEmail(), null, user.getRole().name(), true,
                true);

        String newAccessToken = tokenProvider.generateAccessToken(auth);
        String newRawRefreshToken = tokenProvider.generateRefreshToken(auth);
        String newHash = HashUtils.sha256(newRawRefreshToken);

        // Revoke the old token
        refreshToken.setRevokedAt(OffsetDateTime.now());
        accountApi.saveRefreshToken(refreshToken);

        // Save the new token
        RefreshToken newRefreshTokenModel = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(newHash)
                .familyId(refreshToken.getFamilyId()) // Keep the same family
                .replacedById(refreshToken.getId()) // Link to the old one
                .expiresAt(OffsetDateTime.now().plusDays(jwtProperties.refreshTokenDays()))
                .build();
        accountApi.saveRefreshToken(newRefreshTokenModel);

        return new LoginResult(newAccessToken, newRawRefreshToken, user.getId(), user.getEmail(),
                user.getRole().name());
    }
}
