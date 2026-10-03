package com.danasea.backend.security.authentication.infrastructure.security;

import com.danasea.backend.configs.properties.GoogleOAuth2Properties;
import com.danasea.backend.security.authentication.domain.exceptions.InvalidCredentialsException;
import com.danasea.backend.security.authentication.domain.models.GoogleUserInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoogleTokenVerifierAdapterTest {

    private static final String CLIENT_ID = "test-client-id.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "test-client-secret";

    private MockRestServiceServer mockServer;
    private GoogleTokenVerifierAdapter adapter;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        GoogleOAuth2Properties properties = new GoogleOAuth2Properties(CLIENT_ID, CLIENT_SECRET);
        adapter = new GoogleTokenVerifierAdapter(builder, properties, objectMapper);
    }

    @Test
    void shouldVerifyValidGoogleTokenSuccessfully() {
        String token = "valid-token-123";
        String googleResponseJson = """
            {
                "sub": "google-user-999",
                "email": "user@gmail.com",
                "email_verified": "true",
                "name": "Nguyen Van A",
                "picture": "https://lh3.googleusercontent.com/photo.jpg",
                "aud": "test-client-id.apps.googleusercontent.com"
            }
            """;

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=valid-token-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(googleResponseJson, MediaType.APPLICATION_JSON));

        GoogleUserInfo userInfo = adapter.verify(token);

        assertNotNull(userInfo);
        assertEquals("google-user-999", userInfo.sub());
        assertEquals("user@gmail.com", userInfo.email());
        assertTrue(userInfo.emailVerified());
        assertEquals("Nguyen Van A", userInfo.name());
        assertEquals("https://lh3.googleusercontent.com/photo.jpg", userInfo.picture());
        mockServer.verify();
    }

    @Test
    void shouldThrowWhenTokenIsBlankOrNull() {
        assertThrows(InvalidCredentialsException.class, () -> adapter.verify(null));
        assertThrows(InvalidCredentialsException.class, () -> adapter.verify("   "));
    }

    @Test
    void shouldThrowWhenGoogleReturnsError() {
        String token = "invalid-token";
        String errorJson = """
            {
                "error": "invalid_token",
                "error_description": "Invalid Value"
            }
            """;

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=invalid-token"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(errorJson, MediaType.APPLICATION_JSON));

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> adapter.verify(token));
        assertTrue(ex.getMessage().contains("Invalid Google ID token"));
        mockServer.verify();
    }

    @Test
    void shouldThrowWhenAudienceDoesNotMatchClientId() {
        String token = "mismatched-aud-token";
        String mismatchJson = """
            {
                "sub": "google-user-123",
                "email": "user@gmail.com",
                "email_verified": true,
                "aud": "another-different-client-id.apps.googleusercontent.com"
            }
            """;

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=mismatched-aud-token"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mismatchJson, MediaType.APPLICATION_JSON));

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> adapter.verify(token));
        assertEquals("Google token audience mismatch", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void shouldThrowWhenEmailIsNotVerified() {
        String token = "unverified-email-token";
        String unverifiedJson = """
            {
                "sub": "google-user-123",
                "email": "user@gmail.com",
                "email_verified": false,
                "aud": "test-client-id.apps.googleusercontent.com"
            }
            """;

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=unverified-email-token"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(unverifiedJson, MediaType.APPLICATION_JSON));

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> adapter.verify(token));
        assertEquals("Google email is not verified", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void shouldThrowWhenEmailIsMissing() {
        String token = "missing-email-token";
        String noEmailJson = """
            {
                "sub": "google-user-123",
                "email_verified": true,
                "aud": "test-client-id.apps.googleusercontent.com"
            }
            """;

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=missing-email-token"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(noEmailJson, MediaType.APPLICATION_JSON));

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> adapter.verify(token));
        assertEquals("Google token does not contain email", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void shouldThrowWhenGoogleServerReturnsHttp500() {
        String token = "server-error-token";

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=server-error-token"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> adapter.verify(token));
        assertEquals("Failed to verify Google ID token", ex.getMessage());
        mockServer.verify();
    }
}
