package com.danasea.backend.security.authentication.presentation;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.security.authentication.application.result.LoginResult;
import com.danasea.backend.security.authentication.application.usecase.LoginUseCase;
import com.danasea.backend.security.authentication.application.usecase.RegisterUseCase;
import com.danasea.backend.security.authentication.presentation.dto.AuthenticationResponse;
import com.danasea.backend.security.authentication.presentation.dto.LoginRequest;
import com.danasea.backend.security.authentication.presentation.dto.RefreshResponse;
import com.danasea.backend.security.authentication.presentation.dto.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.http.ResponseEntity;

import com.danasea.backend.security.authentication.application.usecase.RefreshTokenUseCase;
import com.danasea.backend.security.authentication.application.usecase.LogoutUseCase;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @PostMapping("/login")
    public AuthenticationResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = loginUseCase.execute(
                request.email(),
                request.password());

        addRefreshTokenCookie(response, result.refreshToken());

        return new AuthenticationResponse(
                result.accessToken(),
                result.userId(),
                result.email(),
                result.role());
    }

    @PostMapping("/register")
    public AuthenticationResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {

        LoginResult result = registerUseCase.execute(
                request.email(),
                request.password());

        addRefreshTokenCookie(response, result.refreshToken());

        return new AuthenticationResponse(
                result.accessToken(),
                result.userId(),
                result.email(),
                result.role());
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        LoginResult authResponse = refreshTokenUseCase.execute(refreshToken);

        addRefreshTokenCookie(response, authResponse.refreshToken());

        return ResponseEntity.ok(new RefreshResponse(authResponse.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        logoutUseCase.execute(refreshToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }
}
