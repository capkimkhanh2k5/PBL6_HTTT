package com.danasport.backend.authorization.application.usecase;

import java.util.UUID;

import com.danasport.backend.authorization.application.port.AuthorizationPort;
import com.danasport.backend.authorization.application.port.ResourceOwnershipPort;
import com.danasport.backend.authorization.domain.exception.AccessDeniedException;
import com.danasport.backend.authorization.domain.model.AuthorizationSubject;
import com.danasport.backend.authorization.domain.policy.AuthorizationPolicy;

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
