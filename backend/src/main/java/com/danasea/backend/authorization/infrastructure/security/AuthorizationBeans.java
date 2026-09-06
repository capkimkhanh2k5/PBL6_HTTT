package com.danasea.backend.authorization.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.authorization.application.port.AuthorizationPort;
import com.danasea.backend.authorization.application.port.ResourceOwnershipPort;
import com.danasea.backend.authorization.application.usecase.AuthorizationActionUseCase;
import com.danasea.backend.authorization.domain.policy.AuthorizationPolicy;
import com.danasea.backend.authorization.domain.policy.DefaultAuthorizationPolicy;

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
