package com.danasea.backend.security.authentication.application.usecases;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.security.authentication.application.ports.OtpStorePort;
import com.danasea.backend.security.authentication.domain.exceptions.OtpExpiredException;
import com.danasea.backend.security.authentication.domain.exceptions.OtpInvalidException;
import com.danasea.backend.security.authentication.domain.exceptions.OtpMaxAttemptsExceededException;
import com.danasea.backend.security.authentication.domain.models.OtpDetails;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;

import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
public class VerifyOtpUseCase {

    private final AccountInternalApi accountInternalApi;
    private final OtpStorePort otpStorePort;

    private static final int MAX_ATTEMPTS = 5;

    public void execute(String email, String rawOtp) {
        OtpDetails otpDetails = otpStorePort.findByEmail(email)
                .orElseThrow(OtpExpiredException::new);

        if (otpDetails.attempts() >= MAX_ATTEMPTS) {
            otpStorePort.delete(email);
            throw new OtpMaxAttemptsExceededException();
        }

        String hashedOtp = HashUtils.sha256(rawOtp);

        if (!hashedOtp.equals(otpDetails.codeHash())) {
            OtpDetails updatedOtpDetails = new OtpDetails(
                    otpDetails.codeHash(),
                    otpDetails.attempts() + 1,
                    otpDetails.createdAt());

            otpStorePort.save(email, updatedOtpDetails, Duration.ofMinutes(5));
            throw new OtpInvalidException();
        }

        // Success
        User user = accountInternalApi.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsEmailVerified(true);
        accountInternalApi.saveUser(user);

        otpStorePort.delete(email);
    }
}
