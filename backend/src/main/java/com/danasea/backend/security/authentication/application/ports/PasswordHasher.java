package com.danasea.backend.security.authentication.application.ports;

public interface PasswordHasher {
    String hash (String rawPassword);

    boolean matches (String rawPassword, String hash);
}
