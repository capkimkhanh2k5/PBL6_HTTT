package com.danasea.backend.security.authentication.presentation;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.security.authentication.application.results.LoginResult;
import com.danasea.backend.security.authentication.application.usecases.LoginUseCase;
import com.danasea.backend.security.authentication.application.usecases.RegisterUseCase;
import com.danasea.backend.security.authentication.application.usecases.SendVerificationOtpUseCase;
import com.danasea.backend.security.authentication.application.usecases.VerifyOtpUseCase;
import com.danasea.backend.security.authentication.presentation.dtos.AuthenticationResponse;
import com.danasea.backend.security.authentication.presentation.dtos.LoginRequest;
import com.danasea.backend.security.authentication.presentation.dtos.RefreshResponse;
import com.danasea.backend.security.authentication.presentation.dtos.RegisterRequest;
import com.danasea.backend.security.authentication.presentation.dtos.VerifyOtpRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;

import java.security.Principal;

import org.springframework.http.ResponseEntity;

import com.danasea.backend.security.authentication.application.usecases.RefreshTokenUseCase;
import com.danasea.backend.security.authentication.application.usecases.LogoutUseCase;
import com.danasea.backend.security.authentication.infrastructure.security.CookieUtils;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import org.springframework.context.i18n.LocaleContextHolder;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final SendVerificationOtpUseCase sendVerificationOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    @SecurityRequirements()
    public AuthenticationResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = loginUseCase.execute(
                request.email(),
                request.password());

        CookieUtils.addRefreshTokenCookie(response, result.refreshToken(), jwtProperties.refreshTokenMaxAgeSeconds());

        return new AuthenticationResponse(
                result.accessToken(),
                result.userId(),
                result.email(),
                result.role());
    }

    @PostMapping("/register")
    @SecurityRequirements()
    public AuthenticationResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {

        LoginResult result = registerUseCase.execute(
                request.email(),
                request.password(),
                SupportedLanguage.fromTag(LocaleContextHolder.getLocale().toLanguageTag())
                        .orElse(SupportedLanguage.DEFAULT));

        CookieUtils.addRefreshTokenCookie(response, result.refreshToken(), jwtProperties.refreshTokenMaxAgeSeconds());

        return new AuthenticationResponse(
                result.accessToken(),
                result.userId(),
                result.email(),
                result.role());
    }

    @PostMapping("/refresh")
    @SecurityRequirements()
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        LoginResult authResponse = refreshTokenUseCase.execute(refreshToken);

        CookieUtils.addRefreshTokenCookie(
                response, authResponse.refreshToken(), jwtProperties.refreshTokenMaxAgeSeconds());

        return ResponseEntity.ok(new RefreshResponse(authResponse.accessToken()));
    }

    @PostMapping("/logout")
    @SecurityRequirements()
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        logoutUseCase.execute(refreshToken);
        CookieUtils.clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/otp/send")
    public ResponseEntity<Void> sendOtp(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        sendVerificationOtpUseCase.execute(principal.getName());
        return ResponseEntity.accepted().build(); // 202 Accepted
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Void> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        verifyOtpUseCase.execute(principal.getName(), request.code());
        return ResponseEntity.ok().build();
    }
}
