package com.danasport.backend.authentication.application.usecase;

import java.util.Locale;

import com.danasport.backend.authentication.application.port.PasswordHasher;
import com.danasport.backend.authentication.application.port.TokenProvider;
import com.danasport.backend.authentication.application.port.UserAccountPort;
import com.danasport.backend.authentication.application.result.LoginResult;
import com.danasport.backend.authentication.domain.exception.InvalidCredentialsException;
import com.danasport.backend.authentication.domain.model.Authentication;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginUseCase {

    private final UserAccountPort userAccountPort;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;

    public LoginResult execute(String email, String password){
        String normalizedEmail = email.trim()
                                    .toLowerCase(Locale.ROOT);

        Authentication user = userAccountPort.findByEmail(normalizedEmail)
                    .orElseThrow(InvalidCredentialsException :: new);

        if (!user.enabled() 
            || !passwordHasher.matches(
                        password, 
                        user.passwordHash()
                    )){
                throw new InvalidCredentialsException();
            }
            
        String accessToken = tokenProvider.generateAccessToken(user);
            
        return new LoginResult(
            accessToken, 
            user.id(),
            user.email(),
            user.role()
        );
    }
    
}
