package com.danasea.backend.modules.admin.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.admin.presentation.dto.UserSummaryResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetUsersUseCase {

    private final AccountInternalApi accountInternalApi;

    public Page<UserSummaryResponse> execute(Pageable pageable, Role role, Boolean isLocked, String search) {
        Page<User> users = accountInternalApi.findUsers(pageable, role, isLocked, search);
        return users.map(user -> new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getIsEmailVerified(),
                user.getIsLocked(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        ));
    }
}
