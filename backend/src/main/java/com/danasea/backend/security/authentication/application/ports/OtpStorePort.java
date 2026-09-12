package com.danasea.backend.security.authentication.application.ports;

import java.time.Duration;
import java.util.Optional;

import com.danasea.backend.security.authentication.domain.models.OtpDetails;

public interface OtpStorePort {
    void save(String email, OtpDetails details, Duration ttl);
    Optional<OtpDetails> findByEmail(String email);
    void delete(String email);
}
