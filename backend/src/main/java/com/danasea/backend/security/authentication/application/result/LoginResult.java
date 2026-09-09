package com.danasea.backend.security.authentication.application.result;

import java.util.UUID;

public record LoginResult (
    String accessToken,
    String refreshToken,
    UUID userId,
    String email,
    String role
){}
