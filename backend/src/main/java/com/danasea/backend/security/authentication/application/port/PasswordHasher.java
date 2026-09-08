package com.danasea.backend.security.authentication.application.port;

public interface PasswordHasher {
    String hash (String rawPassword);

    boolean matches (String rawPassword, String hash);
}
