package com.danasea.backend.security.authentication.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;

import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class LogoutUseCase {

    private final AccountInternalApi accountApi;

    @Transactional
    public void execute(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash = HashUtils.sha256(rawRefreshToken);
        accountApi.findRefreshTokenByHash(tokenHash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(OffsetDateTime.now());
                accountApi.saveRefreshToken(token);
            }
        });
    }
}
