package com.danasport.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasport.backend.authentication.application.port.PasswordHasher;
import com.danasport.backend.authentication.application.port.TokenProvider;
import com.danasport.backend.authentication.application.port.UserAccountPort;
import com.danasport.backend.authentication.application.usecase.LoginUseCase;
import com.danasport.backend.authentication.application.usecase.RegisterUseCase;

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
