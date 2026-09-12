package com.danasea.backend.modules.account.application.usecases;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.security.authorization.domain.exceptions.AccessDeniedException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetMyProfileUseCase {
    private final AccountInternalApi accountInternalApi;

    public User execute(String email) {
        return accountInternalApi.findUserByEmail(email)
                .orElseThrow(() -> new AccessDeniedException());
    }
}
