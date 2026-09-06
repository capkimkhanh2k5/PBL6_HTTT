package com.danasea.backend.authentication.domain.model;

import java.util.UUID;

public record Authentication(
	UUID id,
	String email,
	String passwordHash,
	String role,
	boolean enabled
) {
}
