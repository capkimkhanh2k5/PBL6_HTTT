package com.danasea.backend.security.authentication.application.usecases;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.security.authentication.application.ports.GoogleTokenVerifierPort;
import com.danasea.backend.security.authentication.application.ports.TokenProvider;
import com.danasea.backend.security.authentication.application.results.LoginResult;
import com.danasea.backend.security.authentication.domain.exceptions.InvalidCredentialsException;
import com.danasea.backend.security.authentication.domain.models.Authentication;
import com.danasea.backend.security.authentication.domain.models.GoogleUserInfo;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class GoogleOAuth2LoginUseCase {

    private final GoogleTokenVerifierPort tokenVerifier;
    private final AccountInternalApi accountInternalApi;
    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    public LoginResult execute(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidCredentialsException("Google ID token is required");
        }

        GoogleUserInfo googleUser = tokenVerifier.verify(idToken);
        String normalizedEmail = googleUser.email().trim().toLowerCase(Locale.ROOT);

        Optional<User> existingUserOpt = accountInternalApi.findUserByEmail(normalizedEmail);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (Boolean.TRUE.equals(user.getIsLocked())) {
                throw new InvalidCredentialsException("Account is locked");
            }

            boolean updated = false;
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && googleUser.picture() != null) {
                user.setAvatarUrl(googleUser.picture());
                updated = true;
            }
            if ((user.getFullName() == null || user.getFullName().isBlank()) && googleUser.name() != null) {
                user.setFullName(googleUser.name());
                updated = true;
            }
            if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
                user.setIsEmailVerified(true);
                updated = true;
            }
            if (updated) {
                user = accountInternalApi.saveUser(user);
            }
        } else {
            // Auto register new customer
            user = new User();
            user.setEmail(normalizedEmail);
            user.setFullName(googleUser.name() != null && !googleUser.name().isBlank() ? googleUser.name() : normalizedEmail);
            user.setAvatarUrl(googleUser.picture());
            user.setRole(Role.CUSTOMER);
            user.setIsEmailVerified(true);
            user.setIsLocked(false);
            user.setLocale("vi");
            user = accountInternalApi.saveUser(user);
            log.info("Auto-registered new user via Google OAuth2: {}", normalizedEmail);
        }

        Authentication auth = new Authentication(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole() != null ? user.getRole().name() : "CUSTOMER",
                !Boolean.TRUE.equals(user.getIsLocked()),
                Boolean.TRUE.equals(user.getIsEmailVerified()),
                user.getLocale() != null ? user.getLocale() : "vi"
        );

        String accessToken = tokenProvider.generateAccessToken(auth);
        String refreshToken = tokenProvider.generateRefreshToken(auth);

        RefreshToken refreshTokenModel = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(HashUtils.sha256(refreshToken))
                .familyId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusDays(jwtProperties.refreshTokenDays()))
                .build();
        accountInternalApi.saveRefreshToken(refreshTokenModel);

        return new LoginResult(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                auth.role()
        );
    }
}
