package com.danasea.backend.security.authentication.application.usecases;

import java.time.Duration;
import java.time.OffsetDateTime;

import com.danasea.backend.security.authentication.application.ports.AuthEventPublisher;
import com.danasea.backend.security.authentication.application.ports.OtpGenerator;
import com.danasea.backend.security.authentication.application.ports.OtpStorePort;
import com.danasea.backend.security.authentication.application.ports.UserAccountPort;
import com.danasea.backend.security.authentication.domain.events.OtpEmailRequestedEvent;
import com.danasea.backend.security.authentication.domain.exceptions.OtpRequestTooFrequentException;
import com.danasea.backend.security.authentication.domain.models.Authentication;
import com.danasea.backend.security.authentication.domain.models.OtpDetails;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SendVerificationOtpUseCase {

    private final UserAccountPort userAccountPort;
    private final OtpStorePort otpStorePort;
    private final AuthEventPublisher authEventPublisher;

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    public void execute(String email) {
        Authentication user = userAccountPort.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.emailVerified()) {
            return; // Don't send OTP if already verified
        }

        otpStorePort.findByEmail(email).ifPresent(otp -> {
            if (Duration.between(otp.createdAt(), OffsetDateTime.now()).compareTo(COOLDOWN) < 0) {
                throw new OtpRequestTooFrequentException();
            }
        });

        String rawOtp = OtpGenerator.generateOtp();
        String hashedOtp = HashUtils.sha256(rawOtp);

        OtpDetails otpDetails = new OtpDetails(
                hashedOtp,
                0,
                OffsetDateTime.now()
        );

        otpStorePort.save(email, otpDetails, OTP_TTL);

        authEventPublisher.publishOtpRequestedEvent(new OtpEmailRequestedEvent(
                user.id(),
                user.email(),
                rawOtp
        ));
    }
}
