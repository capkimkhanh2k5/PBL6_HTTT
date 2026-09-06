package com.danasea.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.MediaType;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import com.danasea.backend.authentication.infrastructure.security.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain applicationSecurityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter
	) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session ->
						session.sessionCreationPolicy(
								SessionCreationPolicy.STATELESS
						))
				.exceptionHandling(exception -> exception
						.accessDeniedHandler((request, response, accessDenied) -> {
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							response.getWriter().write(
									"{\"code\":\"ACCESS_DENIED\",\"message\":\"Access denied\"}"
							);
						}))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/auth/login",
								"/api/auth/register",
								"/actuator/health",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs/**"
						).permitAll()
						.anyRequest()
						.authenticated())
				.addFilterBefore(
						jwtAuthenticationFilter,
						UsernamePasswordAuthenticationFilter.class
				)
				.build();
	}

}
