package com.danasea.backend.security.authentication.infrastructure.security;

import com.danasea.backend.security.authentication.application.ports.TokenProvider;
import com.danasea.backend.security.authentication.application.ports.UserAccountPort;
import com.danasea.backend.security.authentication.domain.models.Authentication;
import com.danasea.backend.security.authorization.application.ports.AuthorizationPort;
import com.danasea.backend.security.authorization.domain.models.AuthorizationSubject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private TokenProvider tokenProvider;
    private UserAccountPort userAccountPort;
    private AuthorizationPort authorizationPort;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(TokenProvider.class);
        userAccountPort = mock(UserAccountPort.class);
        authorizationPort = mock(AuthorizationPort.class);
        filter = new JwtAuthenticationFilter(tokenProvider, userAccountPort, authorizationPort);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unverifiedUserCannotAuthenticateForRegularApi() throws Exception {
        stubUnverifiedUser("/api/users/me");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void unverifiedUserCanAuthenticateOnlyForOtpEndpoints() throws Exception {
        stubUnverifiedUser("/api/auth/otp/send");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    private void stubUnverifiedUser(String requestUri) {
        UUID userId = UUID.randomUUID();
        Authentication user = new Authentication(
                userId, "unverified@example.com", "hash", "CUSTOMER", true, false);
        AuthorizationSubject subject = new AuthorizationSubject(
                userId, user.email(), Set.of("CUSTOMER"), Set.of());

        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(request.getRequestURI()).thenReturn(requestUri);
        when(tokenProvider.getEmail("access-token")).thenReturn(Optional.of(user.email()));
        when(userAccountPort.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(authorizationPort.findSubjectByEmail(user.email())).thenReturn(subject);
    }
}
