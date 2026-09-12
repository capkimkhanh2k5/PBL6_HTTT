package com.danasea.backend.security.authorization.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.security.authorization.application.ports.AuthorizationPort;
import com.danasea.backend.security.authorization.application.ports.ResourceOwnershipPort;
import com.danasea.backend.security.authorization.application.usecases.AuthorizationActionUseCase;
import com.danasea.backend.security.authorization.domain.policies.AuthorizationPolicy;
import com.danasea.backend.security.authorization.domain.policies.DefaultAuthorizationPolicy;

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
