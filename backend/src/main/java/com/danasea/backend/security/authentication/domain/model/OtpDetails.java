package com.danasea.backend.security.authentication.domain.model;

import java.time.OffsetDateTime;

public record OtpDetails(
        String codeHash,
        int attempts,
        OffsetDateTime createdAt
) {
}
