package com.danasea.backend.security.authentication.presentation.dto;

import java.util.UUID;

public record AuthenticationResponse (
    String accessToken,
    UUID userId,
    String email,
    String role
){}
