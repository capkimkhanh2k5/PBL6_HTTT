package com.danasea.backend.security.authentication.domain.models;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticationTest {

    @Test
    void shouldCreateAuthenticationProperly() {
        UUID id = UUID.randomUUID();
        String email = "test@example.com";
        String passwordHash = "hashedPassword";
        String role = "CUSTOMER";
        boolean enabled = true;
        boolean emailVerified = true;

        Authentication auth = new Authentication(id, email, passwordHash, role, enabled, emailVerified);

        assertEquals(id, auth.id());
        assertEquals(email, auth.email());
        assertEquals(passwordHash, auth.passwordHash());
        assertEquals(role, auth.role());
        assertTrue(auth.enabled());
        assertTrue(auth.emailVerified());
    }
}
