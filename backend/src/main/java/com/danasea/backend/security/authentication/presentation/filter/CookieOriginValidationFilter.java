package com.danasea.backend.security.authentication.presentation.filter;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;

import com.danasea.backend.configs.properties.CorsProperties;
import com.danasea.backend.shared.presentation.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieOriginValidationFilter extends OncePerRequestFilter {

    private static final Set<String> COOKIE_ENDPOINTS = Set.of("/api/auth/refresh", "/api/auth/logout");

    private final Set<String> allowedOrigins;
    private final ObjectMapper objectMapper;

    public CookieOriginValidationFilter(
            ObjectProvider<CorsProperties> corsPropertiesProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        CorsProperties corsProperties = corsPropertiesProvider.getIfAvailable(
                () -> new CorsProperties(java.util.List.of()));
        this.allowedOrigins = Set.copyOf(corsProperties.allowedOrigins());
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (!COOKIE_ENDPOINTS.contains(request.getRequestURI())
                || origin == null
                || origin.isBlank()
                || allowedOrigins.contains(origin)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse("INVALID_ORIGIN", "The request origin is not allowed."));
    }
}
