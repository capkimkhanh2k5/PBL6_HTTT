package com.danasport.backend.user.domain.model;

import java.util.UUID;

public record User (
    UUID id,
    String email,
    String passwordHash,
    String role,
    boolean enabled
){}
