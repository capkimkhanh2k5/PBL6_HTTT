package com.danasea.backend.security.authentication.presentation.dtos;

public record GoogleOAuth2Request(
        String idToken,
        String credential
) {
    public String token() {
        if (idToken != null && !idToken.isBlank()) {
            return idToken.trim();
        }
        if (credential != null && !credential.isBlank()) {
            return credential.trim();
        }
        return null;
    }

    public String getEffectiveToken() {
        return token();
    }
}
