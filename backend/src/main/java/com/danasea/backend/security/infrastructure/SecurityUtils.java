package com.danasea.backend.security.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.security.authorization.domain.model.AuthorizationSubject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication.getDetails() instanceof AuthorizationSubject subject) {
            return Optional.ofNullable(subject.userId());
        }

        if (authentication.getPrincipal() instanceof AuthorizationSubject subject) {
            return Optional.ofNullable(subject.userId());
        }

        if (authentication.getName() != null) {
            try {
                return Optional.of(UUID.fromString(authentication.getName()));
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }

        return Optional.empty();
    }

    public static Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication.getDetails() instanceof AuthorizationSubject subject) {
            return Optional.ofNullable(subject.email());
        }

        if (authentication.getPrincipal() instanceof AuthorizationSubject subject) {
            return Optional.ofNullable(subject.email());
        }

        if (authentication.getName() != null && !authentication.getName().isBlank()
                && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return Optional.of(authentication.getName());
        }

        return Optional.empty();
    }
}
