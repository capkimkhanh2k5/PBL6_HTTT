package com.danasea.backend.modules.account.application.service;

import java.util.UUID;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaUserRepository;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.UserJpaEntity;
import com.danasea.backend.modules.account.infrastructure.mapper.UserMapper;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountInternalService implements AccountInternalApi {

    private final JpaUserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findUserById(UUID id) {
        return userRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User saveUser(User user) {
        UserJpaEntity entity = userMapper.toEntity(user);
        UserJpaEntity saved = userRepository.saveAndFlush(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public Optional<Role> findRoleById(UUID roleId) {
        // Role is now an ENUM, this method is deprecated and should not be used.
        return Optional.empty();
    }
}
