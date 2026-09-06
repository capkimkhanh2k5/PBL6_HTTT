package com.danasea.backend.authentication.infrastructure.persistence;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.danasea.backend.authentication.application.port.UserAccountPort;
import com.danasea.backend.authentication.domain.exception.EmailAlreadyUsedException;
import com.danasea.backend.authentication.domain.model.Authentication;
import com.danasea.backend.user.infrastructure.persistence.JpaUserRepository;
import com.danasea.backend.user.infrastructure.persistence.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAccountAdapter implements UserAccountPort {
    
    private final JpaUserRepository repository;


    @Override
    public Optional<Authentication> findByEmail(String email) {
        return repository.findByEmail(email)
            .map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email){
        return repository.existsByEmail(email);
    }

    @Override
    public Authentication save(Authentication user) {
        try {
            UserEntity entity = repository.saveAndFlush(toEntity(user));
            return toDomain(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyUsedException();
        }
    }

    private Authentication toDomain(UserEntity entity) {
        return new Authentication(
            entity.getId(),
            entity.getEmail(),
            entity.getPasswordHash(),
            entity.getRole(),
            entity.isEnabled()
        );
    }

    private UserEntity toEntity(Authentication user) {
        UserEntity entity = new UserEntity();
        
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setRole(user.role());
        entity.setEnabled(user.enabled());

        return entity;
    }
}
