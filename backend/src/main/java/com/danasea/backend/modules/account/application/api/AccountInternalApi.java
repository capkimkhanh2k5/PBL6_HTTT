package com.danasea.backend.modules.account.application.api;

import java.util.UUID;

import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.account.domain.models.Role;

import java.util.Optional;
import java.util.UUID;

public interface AccountInternalApi {
    Optional<User> findUserByEmail(String email);
    Optional<User> findUserById(UUID id);
    boolean existsByEmail(String email);
    User saveUser(User user);
    Optional<Role> findRoleById(UUID roleId);
}
