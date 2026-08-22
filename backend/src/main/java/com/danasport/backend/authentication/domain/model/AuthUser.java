package com.danasport.backend.authentication.domain.model;

import java.util.UUID;

public record AuthUser(
	UUID id,
	String email,
	String passwordHash,
	String role,
	boolean enabled
) {
}
