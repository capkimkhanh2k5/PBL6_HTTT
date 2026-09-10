package com.danasea.backend.modules.admin.presentation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.account.domain.models.Role;

public record UserDetailResponse(
    UUID id,
    String email,
    String phone,
    String fullName,
    Role role,
    String avatarUrl,
    Boolean isEmailVerified,
    Boolean isLocked,
    String locale,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
