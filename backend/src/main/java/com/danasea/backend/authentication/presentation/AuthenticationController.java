package com.danasea.backend.authentication.presentation;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.authentication.application.result.LoginResult;
import com.danasea.backend.authentication.application.usecase.LoginUseCase;
import com.danasea.backend.authentication.application.usecase.RegisterUseCase;
import com.danasea.backend.authentication.presentation.dto.AuthenticationResponse;
import com.danasea.backend.authentication.presentation.dto.LoginRequest;
import com.danasea.backend.authentication.presentation.dto.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;

    @PostMapping("/login")
    public AuthenticationResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginUseCase.execute(
            request.email(),
            request.password() 
        );

        return new AuthenticationResponse(
            result.accessToken(),
            result.userId(),
            result.email(),
            result.role()
        );
    }

    @PostMapping("/register")
    public AuthenticationResponse register(@Valid @RequestBody RegisterRequest request) {
        
        LoginResult result = registerUseCase.execute(
            request.email(),
            request.password()
        );

        return new AuthenticationResponse(
            result.accessToken(),
            result.userId(),
            result.email(),
            result.role()
        );
    }
    
}
