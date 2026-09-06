package com.danasea.backend.authentication.infrastructure.security;

import java.util.List;
import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.danasea.backend.authentication.application.port.TokenProvider;
import com.danasea.backend.authentication.application.port.UserAccountPort;
import com.danasea.backend.authorization.application.port.AuthorizationPort;
import com.danasea.backend.authorization.domain.model.AuthorizationSubject;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter 
            extends OncePerRequestFilter {
    
    private final TokenProvider tokenProvider;
    private final UserAccountPort userAccountPort;
    private final AuthorizationPort authorizationPort;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) 
    throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (SecurityContextHolder.getContext()
                .getAuthentication() == null) {

            tokenProvider.getEmail(token)
                .flatMap(userAccountPort::findByEmail)
                .filter(user -> user.enabled())
                .map(user -> authorizationPort.findSubjectByEmail(user.email()))
                .filter(subject -> !subject.roles().isEmpty())
                .ifPresent(this::authenticate);
        }

        filterChain.doFilter(request, response);
    }

        private void authenticate(AuthorizationSubject subject) {
        List<SimpleGrantedAuthority> authorities = subject.roles()
            .stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        authorities = java.util.stream.Stream.concat(
            authorities.stream(),
            subject.permissions()
                .stream()
                .map(SimpleGrantedAuthority::new)
        ).toList();

        var authentication = new UsernamePasswordAuthenticationToken(
            subject.email(),
            null,
            authorities
        );

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
        }
}
