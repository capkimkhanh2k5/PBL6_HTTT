package com.danasea.backend.security.authentication.infrastructure.security;

import com.danasea.backend.configs.properties.GoogleOAuth2Properties;
import com.danasea.backend.security.authentication.application.ports.GoogleTokenVerifierPort;
import com.danasea.backend.security.authentication.domain.exceptions.InvalidCredentialsException;
import com.danasea.backend.security.authentication.domain.models.GoogleUserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class GoogleTokenVerifierAdapter implements GoogleTokenVerifierPort {

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={token}";

    private final RestClient restClient;
    private final GoogleOAuth2Properties googleProperties;
    private final ObjectMapper objectMapper;

    public GoogleTokenVerifierAdapter(
            RestClient.Builder restClientBuilder,
            GoogleOAuth2Properties googleProperties,
            ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.googleProperties = googleProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidCredentialsException("Google ID token must not be blank");
        }

        try {
            String responseBody = restClient.get()
                    .uri(GOOGLE_TOKENINFO_URL, idToken.trim())
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new InvalidCredentialsException("Empty response from Google token verification");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error")) {
                String error = root.path("error").asText();
                String desc = root.path("error_description").asText();
                log.warn("Google token verification failed: {} - {}", error, desc);
                throw new InvalidCredentialsException("Invalid Google ID token: " + desc);
            }

            // Verify audience against configured Client ID
            String aud = root.path("aud").asText();
            String expectedClientId = googleProperties.clientId();
            if (expectedClientId != null && !expectedClientId.isBlank() && !expectedClientId.equals(aud)) {
                log.warn("Google token audience mismatch. Expected: {}, Got: {}", expectedClientId, aud);
                throw new InvalidCredentialsException("Google token audience mismatch");
            }

            String email = root.path("email").asText();
            if (email == null || email.isBlank()) {
                throw new InvalidCredentialsException("Google token does not contain email");
            }

            boolean emailVerified = root.path("email_verified").asBoolean(false)
                    || "true".equalsIgnoreCase(root.path("email_verified").asText());
            if (!emailVerified) {
                throw new InvalidCredentialsException("Google email is not verified");
            }

            String sub = root.path("sub").asText();
            String name = root.hasNonNull("name") ? root.path("name").asText() : null;
            String picture = root.hasNonNull("picture") ? root.path("picture").asText() : null;

            return new GoogleUserInfo(sub, email, emailVerified, name, picture);
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify Google token with Google servers", e);
            throw new InvalidCredentialsException("Failed to verify Google ID token");
        }
    }
}
