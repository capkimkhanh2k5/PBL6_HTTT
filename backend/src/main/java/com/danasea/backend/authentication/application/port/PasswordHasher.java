package com.danasea.backend.authentication.application.port;

public interface PasswordHasher {
    String hash (String rawPassword);

    boolean matches (String rawPassword, String hash);
}
