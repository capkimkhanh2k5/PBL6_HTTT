package com.danasea.backend.modules.account.presentation.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 255) String fullName,
        @Size(max = 1000) String avatarUrl,
        @Size(max = 10) String locale
) {}
