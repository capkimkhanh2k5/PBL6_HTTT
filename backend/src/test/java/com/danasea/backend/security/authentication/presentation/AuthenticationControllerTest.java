package com.danasea.backend.security.authentication.presentation;

import com.danasea.backend.security.authentication.domain.exception.InvalidCredentialsException;
import com.danasea.backend.security.authentication.domain.exception.OtpInvalidException;
import com.danasea.backend.security.authentication.application.result.LoginResult;
import com.danasea.backend.security.authentication.application.usecase.LoginUseCase;
import com.danasea.backend.security.authentication.application.usecase.LogoutUseCase;
import com.danasea.backend.security.authentication.application.usecase.RefreshTokenUseCase;
import com.danasea.backend.security.authentication.application.usecase.RegisterUseCase;
import com.danasea.backend.security.authentication.application.usecase.VerifyOtpUseCase;
import com.danasea.backend.security.authentication.presentation.dto.LoginRequest;
import com.danasea.backend.security.authentication.presentation.dto.RegisterRequest;
import com.danasea.backend.security.authentication.presentation.dto.VerifyOtpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LoginUseCase loginUseCase;

    @Mock
    private RegisterUseCase registerUseCase;

    @Mock
    private RefreshTokenUseCase refreshTokenUseCase;

    @Mock
    private LogoutUseCase logoutUseCase;

    @Mock
    private VerifyOtpUseCase verifyOtpUseCase;

    @InjectMocks
    private AuthenticationController authenticationController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController)
                .setControllerAdvice(new AuthenticationExceptionHandler(), new com.danasea.backend.shared.presentation.GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldLoginAndReturnCookies() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "Password1!");
        LoginResult result = new LoginResult("access-token", "refresh-token", UUID.randomUUID(), "test@example.com",
                "CUSTOMER");

        when(loginUseCase.execute(anyString(), anyString())).thenReturn(result);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void shouldRegisterAndReturnCookies() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "Password1!");
        LoginResult result = new LoginResult("access-token", "refresh-token", UUID.randomUUID(), "test@example.com",
                "CUSTOMER");

        when(registerUseCase.execute(anyString(), anyString())).thenReturn(result);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        LoginResult result = new LoginResult("new-access-token", "new-refresh-token", UUID.randomUUID(),
                "test@example.com", "CUSTOMER");

        when(refreshTokenUseCase.execute("old-refresh-token")).thenReturn(result);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(cookie().value("refresh_token", "new-refresh-token"));
    }

    @Test
    void shouldFailRefreshWhenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailRefreshWhenTokenInvalidAndClearCookie() throws Exception {
        when(refreshTokenUseCase.execute("invalid-refresh-token"))
                .thenThrow(new InvalidCredentialsException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refresh_token", "invalid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldLogoutAndClearCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .cookie(new Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refresh_token", 0));

        verify(logoutUseCase).execute("old-refresh-token");
    }

    @Test
    void shouldFailVerifyOtpWhenOtpIsInvalid() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("123456");

        doThrow(new OtpInvalidException())
                .when(verifyOtpUseCase).execute(anyString(), eq("123456"));

        // Mock principal for the request since the controller extracts name from
        // principal
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("test@example.com");

        mockMvc.perform(post("/api/auth/otp/verify")
                .principal(mockPrincipal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
    }
}
