package com.danasea.backend.modules.account.presentation.dtos;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        String avatarUrl,
        String locale,
        String role,
        Boolean isEmailVerified
) {}
