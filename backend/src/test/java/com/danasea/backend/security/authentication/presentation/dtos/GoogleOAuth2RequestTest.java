package com.danasea.backend.security.authentication.presentation.dtos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleOAuth2RequestTest {

    @Test
    void shouldReturnIdTokenWhenProvided() {
        GoogleOAuth2Request request = new GoogleOAuth2Request("id-token-xyz", null);
        assertEquals("id-token-xyz", request.token());
        assertEquals("id-token-xyz", request.getEffectiveToken());
    }

    @Test
    void shouldReturnCredentialWhenIdTokenIsBlankOrNull() {
        GoogleOAuth2Request request = new GoogleOAuth2Request(null, "credential-abc");
        assertEquals("credential-abc", request.token());
        assertEquals("credential-abc", request.getEffectiveToken());

        GoogleOAuth2Request requestWithBlankId = new GoogleOAuth2Request("   ", "credential-abc");
        assertEquals("credential-abc", requestWithBlankId.token());
        assertEquals("credential-abc", requestWithBlankId.getEffectiveToken());
    }

    @Test
    void shouldPrioritizeIdTokenOverCredentialWhenBothProvided() {
        GoogleOAuth2Request request = new GoogleOAuth2Request("primary-id-token", "fallback-credential");
        assertEquals("primary-id-token", request.token());
        assertEquals("primary-id-token", request.getEffectiveToken());
    }

    @Test
    void shouldTrimWhitespaceFromTokens() {
        GoogleOAuth2Request request1 = new GoogleOAuth2Request("  trimmed-id-token  ", null);
        assertEquals("trimmed-id-token", request1.token());

        GoogleOAuth2Request request2 = new GoogleOAuth2Request(null, "  trimmed-credential  ");
        assertEquals("trimmed-credential", request2.token());
    }

    @Test
    void shouldReturnNullWhenBothTokensAreNullOrBlank() {
        GoogleOAuth2Request request1 = new GoogleOAuth2Request(null, null);
        assertNull(request1.token());
        assertNull(request1.getEffectiveToken());

        GoogleOAuth2Request request2 = new GoogleOAuth2Request("   ", "  ");
        assertNull(request2.token());
        assertNull(request2.getEffectiveToken());
    }
}
