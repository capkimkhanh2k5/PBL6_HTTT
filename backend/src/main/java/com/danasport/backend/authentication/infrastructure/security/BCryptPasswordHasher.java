package com.danasport.backend.authentication.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.danasport.backend.authentication.application.port.PasswordHasher;

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
