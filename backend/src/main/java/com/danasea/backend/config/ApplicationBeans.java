package com.danasea.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.authentication.application.port.PasswordHasher;
import com.danasea.backend.authentication.application.port.TokenProvider;
import com.danasea.backend.authentication.application.port.UserAccountPort;
import com.danasea.backend.authentication.application.usecase.LoginUseCase;
import com.danasea.backend.authentication.application.usecase.RegisterUseCase;

@Configuration
public class ApplicationBeans {
    
    @Bean
    LoginUseCase loginUseCase(
            UserAccountPort userAccountPort,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider
    ) {
        return new LoginUseCase(
                userAccountPort,
                passwordHasher,
                tokenProvider
        );
    }

    @Bean
    RegisterUseCase registerUseCase(
            UserAccountPort userAccountPort,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider
    ) {
        return new RegisterUseCase(
                userAccountPort,
                passwordHasher,
                tokenProvider
        );
    }
}
