package com.danasea.backend.modules.systemconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.security.authentication.application.port.PasswordHasher;
import com.danasea.backend.security.authentication.application.port.TokenProvider;
import com.danasea.backend.security.authentication.application.port.UserAccountPort;
import com.danasea.backend.security.authentication.application.usecase.LoginUseCase;
import com.danasea.backend.security.authentication.application.usecase.RegisterUseCase;
import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import org.springframework.context.ApplicationEventPublisher;

import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;
import com.danasea.backend.security.authentication.application.usecase.RefreshTokenUseCase;
import com.danasea.backend.security.authentication.application.usecase.LogoutUseCase;

@Configuration
public class ApplicationBeans {
    
    @Bean
    LoginUseCase loginUseCase(
            UserAccountPort userAccountPort,
            AccountInternalApi accountInternalApi,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            JwtProperties jwtProperties
    ) {
        return new LoginUseCase(
                userAccountPort,
                accountInternalApi,
                passwordHasher,
                tokenProvider,
                jwtProperties
        );
    }

    @Bean
    RegisterUseCase registerUseCase(
            UserAccountPort userAccountPort,
            AccountInternalApi accountInternalApi,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            JwtProperties jwtProperties,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        return new RegisterUseCase(
                userAccountPort,
                accountInternalApi,
                applicationEventPublisher,
                passwordHasher,
                tokenProvider,
                jwtProperties
        );
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(
            AccountInternalApi accountInternalApi,
            TokenProvider tokenProvider,
            JwtProperties jwtProperties
    ) {
        return new RefreshTokenUseCase(accountInternalApi, tokenProvider, jwtProperties);
    }

    @Bean
    LogoutUseCase logoutUseCase(AccountInternalApi accountInternalApi) {
        return new LogoutUseCase(accountInternalApi);
    }
}
