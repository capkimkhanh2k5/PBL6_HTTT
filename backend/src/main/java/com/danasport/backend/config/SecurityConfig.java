package com.danasport.backend.config;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	@Order(0)
	SecurityFilterChain actuatorHealthSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.securityMatcher(EndpointRequest.to("health"))
				.authorizeHttpRequests(authorize -> authorize
						.anyRequest().permitAll())
				.build();
	}

	@Bean
	@Order(1)
	SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(authorize -> authorize
						.anyRequest().authenticated())
				.httpBasic(Customizer.withDefaults())
				.formLogin(Customizer.withDefaults())
				.build();
	}

}
