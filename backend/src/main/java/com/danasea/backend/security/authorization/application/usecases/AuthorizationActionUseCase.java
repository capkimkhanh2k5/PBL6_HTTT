package com.danasea.backend.security.authorization.application.usecases;

import java.util.UUID;

import com.danasea.backend.security.authorization.application.ports.AuthorizationPort;
import com.danasea.backend.security.authorization.application.ports.ResourceOwnershipPort;
import com.danasea.backend.security.authorization.domain.exceptions.AccessDeniedException;
import com.danasea.backend.security.authorization.domain.models.AuthorizationSubject;
import com.danasea.backend.security.authorization.domain.policies.AuthorizationPolicy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthorizationActionUseCase {
    
    private final AuthorizationPort authorizationPort;
    private final ResourceOwnershipPort resourceOwnershipPort;
    private final AuthorizationPolicy authorizationPolicy;

    public void check(
        UUID userId,
        String permission,
        String resourceType,
        UUID resourceId
    ) {
        AuthorizationSubject subject = authorizationPort
            .findSubjectByUserId(userId);

        boolean ownsResource = resourceId != null
            && resourceOwnershipPort.isOwner(
                userId,
                resourceType,
                resourceId
            );

        if (!authorizationPolicy.can(subject, permission, ownsResource)) {
            throw new AccessDeniedException();
        }
    }
}
