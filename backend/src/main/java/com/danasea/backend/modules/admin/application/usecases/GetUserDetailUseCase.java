package com.danasea.backend.modules.admin.application.usecases;

import java.util.UUID;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.admin.domain.exceptions.UserNotFoundException;
import com.danasea.backend.modules.admin.presentation.dtos.UserDetailResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetUserDetailUseCase {

    private final AccountInternalApi accountInternalApi;

    public UserDetailResponse execute(UUID id) {
        User user = accountInternalApi.findUserById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        return new UserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getIsEmailVerified(),
                user.getIsLocked(),
                user.getLocale(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
