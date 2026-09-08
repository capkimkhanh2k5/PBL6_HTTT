package com.danasea.backend.security.authentication.application.result;

import java.util.UUID;

public record LoginResult (
    String accessToken,
    UUID userId,
    String email,
    String role
){}
