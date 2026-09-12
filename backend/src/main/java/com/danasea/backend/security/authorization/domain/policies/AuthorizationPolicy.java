package com.danasea.backend.security.authorization.domain.policies;

import com.danasea.backend.security.authorization.domain.models.AuthorizationSubject;

public interface AuthorizationPolicy {

    boolean can(
            AuthorizationSubject subject, 
            String permission, 
            boolean ownsResource
        );
}
