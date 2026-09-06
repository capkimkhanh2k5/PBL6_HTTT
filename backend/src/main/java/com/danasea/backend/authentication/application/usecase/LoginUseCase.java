package com.danasea.backend.authentication.application.usecase;

import java.util.Locale;

import com.danasea.backend.authentication.application.port.PasswordHasher;
import com.danasea.backend.authentication.application.port.TokenProvider;
import com.danasea.backend.authentication.application.port.UserAccountPort;
import com.danasea.backend.authentication.application.result.LoginResult;
import com.danasea.backend.authentication.domain.exception.InvalidCredentialsException;
import com.danasea.backend.authentication.domain.model.Authentication;

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
