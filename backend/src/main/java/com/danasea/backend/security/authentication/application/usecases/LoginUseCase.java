package com.danasea.backend.security.authentication.application.usecases;

import java.util.Locale;

import com.danasea.backend.security.authentication.application.ports.PasswordHasher;
import com.danasea.backend.security.authentication.application.ports.TokenProvider;
import com.danasea.backend.security.authentication.application.ports.UserAccountPort;
import com.danasea.backend.security.authentication.application.results.LoginResult;
import com.danasea.backend.security.authentication.domain.exceptions.InvalidCredentialsException;
import com.danasea.backend.security.authentication.domain.models.Authentication;

import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.time.OffsetDateTime;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;

@RequiredArgsConstructor
public class LoginUseCase {

    private final UserAccountPort userAccountPort;
    private final AccountInternalApi accountInternalApi;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    public LoginResult execute(String email, String password) {
        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        Authentication user = userAccountPort.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.enabled()
                || !user.emailVerified()
                || !passwordHasher.matches(
                        password,
                        user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        RefreshToken refreshTokenModel = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.id())
                .tokenHash(HashUtils.sha256(refreshToken))
                .familyId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusDays(jwtProperties.refreshTokenDays()))
                .build();
        accountInternalApi.saveRefreshToken(refreshTokenModel);

        return new LoginResult(
                accessToken,
                refreshToken,
                user.id(),
                user.email(),
                user.role());
    }

}
