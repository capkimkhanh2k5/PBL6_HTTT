package com.danasea.backend.security.authentication.application.usecase;

import java.util.Locale;

import com.danasea.backend.security.authentication.application.port.PasswordHasher;
import com.danasea.backend.security.authentication.application.port.TokenProvider;
import com.danasea.backend.security.authentication.application.port.UserAccountPort;
import com.danasea.backend.security.authentication.application.result.LoginResult;
import com.danasea.backend.security.authentication.domain.exception.EmailAlreadyUsedException;
import com.danasea.backend.security.authentication.domain.model.Authentication;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterUseCase {
    
    private final UserAccountPort userAccountPort;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;

    public LoginResult execute(String email, String password){
        String normalizedEmail = email.trim()
                                    .toLowerCase(Locale.ROOT);

        if (userAccountPort.existsByEmail(normalizedEmail)){
            throw new EmailAlreadyUsedException();
        }

        String hashedPassword = passwordHasher.hash(password);
        
        Authentication user = new Authentication(
            null, 
            normalizedEmail, 
            hashedPassword,
            "USER",
            true
        );

        Authentication savedUser = userAccountPort.save(user);

        String accessToken = tokenProvider.generateAccessToken(savedUser);

        return new LoginResult(
            accessToken, 
            savedUser.id(),
            savedUser.email(),
            savedUser.role()
        );
    }
}
