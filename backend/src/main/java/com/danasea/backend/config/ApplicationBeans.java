package com.danasea.backend.modules.systemconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.security.authentication.application.port.AuthEventPublisher;
import com.danasea.backend.security.authentication.application.port.OtpStorePort;
import com.danasea.backend.security.authentication.application.port.PasswordHasher;
import com.danasea.backend.security.authentication.application.port.TokenProvider;
import com.danasea.backend.security.authentication.application.port.UserAccountPort;
import com.danasea.backend.security.authentication.application.usecase.LoginUseCase;
import com.danasea.backend.security.authentication.application.usecase.RegisterUseCase;
import com.danasea.backend.security.authentication.application.usecase.SendVerificationOtpUseCase;
import com.danasea.backend.security.authentication.application.usecase.VerifyOtpUseCase;
import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.application.usecase.ChangePasswordUseCase;
import com.danasea.backend.modules.account.application.usecase.GetMyProfileUseCase;
import com.danasea.backend.modules.account.application.usecase.UpdateProfileUseCase;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaAuditLogRepository;

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
            JwtProperties jwtProperties) {
        return new LoginUseCase(
                userAccountPort,
                accountInternalApi,
                passwordHasher,
                tokenProvider,
                jwtProperties);
    }

    @Bean
    RegisterUseCase registerUseCase(
            UserAccountPort userAccountPort,
            AccountInternalApi accountInternalApi,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            JwtProperties jwtProperties,
            ApplicationEventPublisher applicationEventPublisher) {
        return new RegisterUseCase(
                userAccountPort,
                accountInternalApi,
                applicationEventPublisher,
                passwordHasher,
                tokenProvider,
                jwtProperties);
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(
            AccountInternalApi accountInternalApi,
            TokenProvider tokenProvider,
            JwtProperties jwtProperties) {
        return new RefreshTokenUseCase(accountInternalApi, tokenProvider, jwtProperties);
    }

    @Bean
    LogoutUseCase logoutUseCase(AccountInternalApi accountInternalApi) {
        return new LogoutUseCase(accountInternalApi);
    }

    @Bean
    SendVerificationOtpUseCase sendVerificationOtpUseCase(
            UserAccountPort userAccountPort,
            OtpStorePort otpStorePort,
            AuthEventPublisher authEventPublisher) {
        return new com.danasea.backend.security.authentication.application.usecase.SendVerificationOtpUseCase(
                userAccountPort, otpStorePort, authEventPublisher);
    }

    @Bean
    VerifyOtpUseCase verifyOtpUseCase(
            AccountInternalApi accountInternalApi,
            OtpStorePort otpStorePort) {
        return new VerifyOtpUseCase(accountInternalApi,
                otpStorePort);
    }

    @Bean
    GetMyProfileUseCase getMyProfileUseCase(AccountInternalApi accountInternalApi) {
        return new GetMyProfileUseCase(accountInternalApi);
    }

    @Bean
    UpdateProfileUseCase updateProfileUseCase(AccountInternalApi accountInternalApi) {
        return new UpdateProfileUseCase(accountInternalApi);
    }

    @Bean
    ChangePasswordUseCase changePasswordUseCase(
            AccountInternalApi accountInternalApi,
            PasswordHasher passwordHasher,
            JpaAuditLogRepository auditLogRepository) {
        return new ChangePasswordUseCase(accountInternalApi, passwordHasher, auditLogRepository);
    }
}
