package com.danasea.backend.security.authentication.application.usecase;

import java.util.Locale;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.security.authentication.application.port.PasswordHasher;
import com.danasea.backend.security.authentication.application.port.TokenProvider;
import com.danasea.backend.security.authentication.application.port.UserAccountPort;
import com.danasea.backend.security.authentication.application.result.LoginResult;
import com.danasea.backend.security.authentication.domain.event.UserRegisteredEvent;
import com.danasea.backend.security.authentication.domain.exception.EmailAlreadyUsedException;
import com.danasea.backend.security.authentication.domain.model.Authentication;

import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.time.OffsetDateTime;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;

@RequiredArgsConstructor
public class RegisterUseCase {

    private final UserAccountPort userAccountPort;
    private final AccountInternalApi accountInternalApi;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional
    public LoginResult execute(String email, String password) {
        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        if (userAccountPort.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyUsedException();
        }

        String hashedPassword = passwordHasher.hash(password);

        Authentication user = new Authentication(
                null,
                normalizedEmail,
                hashedPassword,
                "CUSTOMER",
                true, // enabled
                false // emailVerified
        );

        Authentication savedUser = userAccountPort.save(user);

        String accessToken = tokenProvider.generateAccessToken(savedUser);
        String refreshToken = tokenProvider.generateRefreshToken(savedUser);

        RefreshToken refreshTokenModel = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(savedUser.id())
                .tokenHash(HashUtils.sha256(refreshToken))
                .familyId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusDays(jwtProperties.refreshTokenDays()))
                .build();
        accountInternalApi.saveRefreshToken(refreshTokenModel);

        // Publish event
        applicationEventPublisher.publishEvent(new UserRegisteredEvent(
                UUID.randomUUID(),
                savedUser.id(),
                savedUser.email(),
                savedUser.role(),
                OffsetDateTime.now()));

        return new LoginResult(
                accessToken,
                refreshToken,
                savedUser.id(),
                savedUser.email(),
                savedUser.role());
    }
}
