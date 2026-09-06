package com.danasea.backend.authentication.application.result;

import java.util.UUID;

public record LoginResult (
    String accessToken,
    UUID userId,
    String email,
    String role
){}
