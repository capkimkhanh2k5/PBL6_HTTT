package com.danasport.backend.authentication.infrastructure.security;

import java.util.List;
import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.danasport.backend.authentication.application.port.TokenProvider;
import com.danasport.backend.authentication.application.port.UserAccountPort;

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
                .ifPresent(user -> {
                    var authentication = new UsernamePasswordAuthenticationToken(
                                                                user.email(), 
                                                                null, 
                                                                List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))
                                                            );
                    SecurityContextHolder.getContext()
                                            .setAuthentication(authentication);
                });
        }

        filterChain.doFilter(request, response);
    }
}
