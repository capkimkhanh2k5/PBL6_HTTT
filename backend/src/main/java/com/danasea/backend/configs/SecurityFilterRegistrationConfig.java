package com.danasea.backend.configs;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.security.authentication.infrastructure.security.JwtAuthenticationFilter;
import com.danasea.backend.security.authentication.presentation.filter.CookieOriginValidationFilter;
import com.danasea.backend.security.authentication.presentation.filter.OtpRateLimitFilter;
import com.danasea.backend.security.authentication.presentation.filter.RateLimitFilter;
import com.danasea.backend.shared.infrastructure.web.RequestIdFilter;

import jakarta.servlet.Filter;

@Configuration
public class SecurityFilterRegistrationConfig {

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        return disabled(filter);
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        return disabled(filter);
    }

    @Bean
    FilterRegistrationBean<OtpRateLimitFilter> otpRateLimitFilterRegistration(OtpRateLimitFilter filter) {
        return disabled(filter);
    }

    @Bean
    FilterRegistrationBean<CookieOriginValidationFilter> cookieOriginFilterRegistration(
            CookieOriginValidationFilter filter) {
        return disabled(filter);
    }

    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration(RequestIdFilter filter) {
        return disabled(filter);
    }

    private <T extends Filter> FilterRegistrationBean<T> disabled(T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
