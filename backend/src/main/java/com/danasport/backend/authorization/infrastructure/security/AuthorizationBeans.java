package com.danasport.backend.authorization.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasport.backend.authorization.application.port.AuthorizationPort;
import com.danasport.backend.authorization.application.port.ResourceOwnershipPort;
import com.danasport.backend.authorization.application.usecase.AuthorizationActionUseCase;
import com.danasport.backend.authorization.domain.policy.AuthorizationPolicy;
import com.danasport.backend.authorization.domain.policy.DefaultAuthorizationPolicy;

@Configuration
public class AuthorizationBeans {

	@Bean
	AuthorizationPolicy authorizationPolicy() {
		return new DefaultAuthorizationPolicy();
	}

	@Bean
	AuthorizationActionUseCase authorizationActionUseCase(
			AuthorizationPort authorizationPort,
			ResourceOwnershipPort resourceOwnershipPort,
			AuthorizationPolicy authorizationPolicy
	) {
		return new AuthorizationActionUseCase(
				authorizationPort,
				resourceOwnershipPort,
				authorizationPolicy
		);
	}
}
