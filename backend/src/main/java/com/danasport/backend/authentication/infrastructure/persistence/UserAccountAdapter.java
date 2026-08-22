package com.danasport.backend.authentication.infrastructure.persistence;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.danasport.backend.authentication.application.port.UserAccountPort;
import com.danasport.backend.authentication.domain.exception.EmailAlreadyUsedException;
import com.danasport.backend.authentication.domain.model.AuthUser;
import com.danasport.backend.user.infrastructure.persistence.JpaUserRepository;
import com.danasport.backend.user.infrastructure.persistence.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAccountAdapter implements UserAccountPort {
    
    private final JpaUserRepository repository;


    @Override
    public Optional<AuthUser> findByEmail(String email) {
        return repository.findByEmail(email)
            .map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email){
        return repository.existsByEmail(email);
    }

    @Override
    public AuthUser save(AuthUser user) {
        try {
            UserEntity entity = repository.saveAndFlush(toEntity(user));
            return toDomain(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyUsedException();
        }
    }

    private AuthUser toDomain(UserEntity entity) {
        return new AuthUser(
            entity.getId(),
            entity.getEmail(),
            entity.getPasswordHash(),
            entity.getRole(),
            entity.isEnabled()
        );
    }

    private UserEntity toEntity(AuthUser user) {
        UserEntity entity = new UserEntity();
        
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setRole(user.role());
        entity.setEnabled(user.enabled());

        return entity;
    }
}
