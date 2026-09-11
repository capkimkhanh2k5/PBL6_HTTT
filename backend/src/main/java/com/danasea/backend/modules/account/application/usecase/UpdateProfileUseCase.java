package com.danasea.backend.modules.account.application.usecase;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.security.authorization.domain.exception.AccessDeniedException;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class UpdateProfileUseCase {
    private final AccountInternalApi accountInternalApi;

    @Transactional
    public User execute(String email, String fullName, String avatarUrl, String locale) {
        User user = accountInternalApi.findUserByEmail(email)
                .orElseThrow(() -> new AccessDeniedException());
        
        if (fullName != null) {
            user.setFullName(fullName);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        if (locale != null) {
            user.setLocale(locale);
        }
        
        return accountInternalApi.saveUser(user);
    }
}
