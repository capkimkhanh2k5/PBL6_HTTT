package com.danasea.backend.security.authentication.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleUserInfoTest {

    @Test
    void shouldCreateGoogleUserInfoRecordProperly() {
        GoogleUserInfo info = new GoogleUserInfo(
                "sub-12345",
                "test@example.com",
                true,
                "Test User",
                "https://example.com/avatar.jpg"
        );

        assertEquals("sub-12345", info.sub());
        assertEquals("test@example.com", info.email());
        assertTrue(info.emailVerified());
        assertEquals("Test User", info.name());
        assertEquals("https://example.com/avatar.jpg", info.picture());
    }

    @Test
    void shouldSupportNullOptionalFields() {
        GoogleUserInfo info = new GoogleUserInfo(
                "sub-12345",
                "test@example.com",
                false,
                null,
                null
        );

        assertEquals("sub-12345", info.sub());
        assertEquals("test@example.com", info.email());
        assertFalse(info.emailVerified());
        assertNull(info.name());
        assertNull(info.picture());
    }
}
