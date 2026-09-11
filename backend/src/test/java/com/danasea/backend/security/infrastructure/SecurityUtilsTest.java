package com.danasea.backend.security.infrastructure;

import com.danasea.backend.security.authorization.domain.model.AuthorizationSubject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_whenNotAuthenticated_returnsEmpty() {
        SecurityContextHolder.clearContext();

        Optional<UUID> userId = SecurityUtils.getCurrentUserId();

        assertTrue(userId.isEmpty());
    }

    @Test
    void getCurrentUserId_whenDetailsHasAuthorizationSubject_returnsUserId() {
        UUID expectedUserId = UUID.randomUUID();
        AuthorizationSubject subject = new AuthorizationSubject(
                expectedUserId,
                "user@example.com",
                Set.of("CUSTOMER"),
                Set.of("READ")
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                subject.email(), null, java.util.Collections.emptyList()
        );
        auth.setDetails(subject);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UUID> result = SecurityUtils.getCurrentUserId();

        assertTrue(result.isPresent());
        assertEquals(expectedUserId, result.get());
    }

    @Test
    void getCurrentUserId_whenPrincipalHasAuthorizationSubject_returnsUserId() {
        UUID expectedUserId = UUID.randomUUID();
        AuthorizationSubject subject = new AuthorizationSubject(
                expectedUserId,
                "user2@example.com",
                Set.of("CUSTOMER"),
                Set.of("READ")
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                subject, null, java.util.Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UUID> result = SecurityUtils.getCurrentUserId();

        assertTrue(result.isPresent());
        assertEquals(expectedUserId, result.get());
    }

    @Test
    void getCurrentUserEmail_whenDetailsPresent_returnsEmail() {
        AuthorizationSubject subject = new AuthorizationSubject(
                UUID.randomUUID(),
                "test@example.com",
                Set.of("CUSTOMER"),
                Set.of("READ")
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                subject.email(), null, java.util.Collections.emptyList()
        );
        auth.setDetails(subject);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> email = SecurityUtils.getCurrentUserEmail();

        assertTrue(email.isPresent());
        assertEquals("test@example.com", email.get());
    }
}
