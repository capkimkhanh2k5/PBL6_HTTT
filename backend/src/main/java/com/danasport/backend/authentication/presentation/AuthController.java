package com.danasport.backend.authentication.presentation;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasport.backend.authentication.application.result.LoginResult;
import com.danasport.backend.authentication.application.usecase.LoginUseCase;
import com.danasport.backend.authentication.application.usecase.RegisterUseCase;
import com.danasport.backend.authentication.presentation.dto.AuthResponse;
import com.danasport.backend.authentication.presentation.dto.LoginRequest;
import com.danasport.backend.authentication.presentation.dto.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginUseCase.execute(
            request.email(),
            request.password() 
        );

        return new AuthResponse(
            result.accessToken(),
            result.userId(),
            result.email(),
            result.role()
        );
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        
        LoginResult result = registerUseCase.execute(
            request.email(),
            request.password()
        );

        return new AuthResponse(
            result.accessToken(),
            result.userId(),
            result.email(),
            result.role()
        );
    }
    
}
