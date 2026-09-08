package com.danasea.backend.security.authorization.domain.policy;

import com.danasea.backend.security.authorization.domain.model.AuthorizationSubject;

public interface AuthorizationPolicy {

    boolean can(
            AuthorizationSubject subject, 
            String permission, 
            boolean ownsResource
        );
}
