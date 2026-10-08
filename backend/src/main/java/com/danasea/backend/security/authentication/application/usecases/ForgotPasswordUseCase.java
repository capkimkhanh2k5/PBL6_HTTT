package com.danasea.backend.security.authentication.application.usecases;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.security.authentication.application.ports.AuthEventPublisher;
import com.danasea.backend.security.authentication.application.ports.OtpGenerator;
import com.danasea.backend.security.authentication.domain.events.PasswordResetRequestedEvent;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordUseCase {

    private final AccountInternalApi accountInternalApi;
    private final AuthEventPublisher authEventPublisher;

    private static final int OTP_EXPIRATION_MINUTES = 15;

    @Transactional
    public void execute(String email, SupportedLanguage language) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Optional<User> optionalUser = accountInternalApi.findUserByEmail(normalizedEmail);

        if (optionalUser.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", normalizedEmail);
            return;
        }

        User user = optionalUser.get();
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            log.warn("Password reset requested for locked user: {}", normalizedEmail);
            return;
        }

        // Invalidate previous unused reset tokens for this user
        accountInternalApi.invalidatePasswordResetTokens(user.getId());

        // Generate 6-digit OTP and store SHA-256 hash
        String rawOtp = OtpGenerator.generateOtp();
        String hashedOtp = HashUtils.sha256(rawOtp);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);

        accountInternalApi.createPasswordResetToken(user.getId(), hashedOtp, expiresAt);

        String locale = (language != null ? language : SupportedLanguage.DEFAULT).code();
        authEventPublisher.publishPasswordResetRequestedEvent(new PasswordResetRequestedEvent(
                user.getId(),
                user.getEmail(),
                rawOtp,
                locale
        ));

        log.info("Password reset OTP generated and event published for userId: {}", user.getId());
    }
}
