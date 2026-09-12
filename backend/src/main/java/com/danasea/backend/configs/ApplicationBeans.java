package com.danasea.backend.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.security.authentication.application.ports.AuthEventPublisher;
import com.danasea.backend.security.authentication.application.ports.OtpStorePort;
import com.danasea.backend.security.authentication.application.ports.PasswordHasher;
import com.danasea.backend.security.authentication.application.ports.TokenProvider;
import com.danasea.backend.security.authentication.application.ports.UserAccountPort;
import com.danasea.backend.security.authentication.application.usecases.LoginUseCase;
import com.danasea.backend.security.authentication.application.usecases.RegisterUseCase;
import com.danasea.backend.security.authentication.application.usecases.SendVerificationOtpUseCase;
import com.danasea.backend.security.authentication.application.usecases.VerifyOtpUseCase;
import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.application.usecases.ChangePasswordUseCase;
import com.danasea.backend.modules.account.application.usecases.GetMyProfileUseCase;
import com.danasea.backend.modules.account.application.usecases.UpdateProfileUseCase;

import org.springframework.context.ApplicationEventPublisher;

import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;
import com.danasea.backend.security.authentication.application.usecases.RefreshTokenUseCase;
import com.danasea.backend.security.authentication.application.usecases.LogoutUseCase;
import com.danasea.backend.modules.admin.application.usecases.GetUserDetailUseCase;
import com.danasea.backend.modules.admin.application.usecases.GetUsersUseCase;
import com.danasea.backend.modules.admin.application.usecases.LockUserUseCase;
import com.danasea.backend.modules.admin.application.usecases.UnlockUserUseCase;
import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ApplicationBeans {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

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
        return new SendVerificationOtpUseCase(
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
    LockUserUseCase lockUserUseCase(AccountInternalApi accountInternalApi, AuditLogInternalApi auditLogInternalApi) {
        return new LockUserUseCase(accountInternalApi, auditLogInternalApi);
    }

    @Bean
    UnlockUserUseCase unlockUserUseCase(AccountInternalApi accountInternalApi, AuditLogInternalApi auditLogInternalApi) {
        return new UnlockUserUseCase(accountInternalApi, auditLogInternalApi);
    }

    @Bean
    GetUsersUseCase getUsersUseCase(AccountInternalApi accountInternalApi) {
        return new GetUsersUseCase(accountInternalApi);
    }

    @Bean
    GetUserDetailUseCase getUserDetailUseCase(AccountInternalApi accountInternalApi) {
        return new GetUserDetailUseCase(accountInternalApi);
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
            AuditLogInternalApi auditLogInternalApi) {
        return new ChangePasswordUseCase(accountInternalApi, passwordHasher, auditLogInternalApi);
    }
}
