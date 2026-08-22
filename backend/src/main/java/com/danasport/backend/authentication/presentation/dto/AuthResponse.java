package com.danasport.backend.authentication.presentation.dto;

import java.util.UUID;

public record AuthResponse (
    String accessToken,
    UUID userId,
    String email,
    String role
){}
