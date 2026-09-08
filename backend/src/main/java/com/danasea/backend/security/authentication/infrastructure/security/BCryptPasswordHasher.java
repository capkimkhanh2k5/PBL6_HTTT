package com.danasea.backend.security.authentication.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.danasea.backend.security.authentication.application.port.PasswordHasher;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hash(String rawPassword){
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return passwordEncoder.matches(rawPassword, hash);
    }
}
