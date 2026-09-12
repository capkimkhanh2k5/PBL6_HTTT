package com.danasea.backend.security.authentication.infrastructure.persistence;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.danasea.backend.security.authentication.application.ports.UserAccountPort;
import com.danasea.backend.security.authentication.domain.exceptions.EmailAlreadyUsedException;
import com.danasea.backend.security.authentication.domain.models.Authentication;
import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.account.domain.models.Role;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAccountAdapter implements UserAccountPort {

    private final AccountInternalApi accountApi;

    @Override
    public Optional<Authentication> findByEmail(String email) {
        return accountApi.findUserByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return accountApi.existsByEmail(email);
    }

    @Override
    public Authentication save(Authentication auth) {
        try {
            User user = toUser(auth);
            User savedUser = accountApi.saveUser(user);
            return toDomain(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyUsedException();
        }
    }

    private Authentication toDomain(User user) {
        return new Authentication(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                (user.getRole() != null ? user.getRole().name() : "CUSTOMER"),
                !Boolean.TRUE.equals(user.getIsLocked()),
                Boolean.TRUE.equals(user.getIsEmailVerified()));
    }

    private User toUser(Authentication auth) {
        User user = new User();
        user.setId(auth.id());
        user.setEmail(auth.email());
        user.setPasswordHash(auth.passwordHash());
        user.setIsLocked(!auth.enabled());
        user.setIsEmailVerified(auth.emailVerified());
        try {
            user.setRole(Role.valueOf(auth.role()));
        } catch (Exception e) {
            user.setRole(Role.CUSTOMER);
        }
        return user;
    }
}
