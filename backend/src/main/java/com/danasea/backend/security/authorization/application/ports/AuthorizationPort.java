package com.danasea.backend.security.authorization.application.ports;

import java.util.Set;
import java.util.UUID;

import com.danasea.backend.security.authorization.domain.models.AuthorizationSubject;

public interface AuthorizationPort {

    AuthorizationSubject findSubjectByEmail(String email);

    AuthorizationSubject findSubjectByUserId(UUID userId);

    Set<String> findRolesByUserId (UUID userId);

    Set<String> findPermissionsByUserId (UUID userId);

    boolean hasPermission (UUID userId, String permissionCode);
}
