package com.danasport.backend.authorization.application.port;

import java.util.Set;
import java.util.UUID;

import com.danasport.backend.authorization.domain.model.AuthorizationSubject;

public interface AuthorizationPort {

    AuthorizationSubject findSubjectByEmail(String email);

    AuthorizationSubject findSubjectByUserId(UUID userId);

    Set<String> findRolesByUserId (UUID userId);

    Set<String> findPermissionsByUserId (UUID userId);

    boolean hasPermission (UUID userId, String permissionCode);
}
