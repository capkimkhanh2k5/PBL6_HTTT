package com.danasea.backend.authorization.domain.policy;

import com.danasea.backend.authorization.domain.model.AuthorizationSubject;

public interface AuthorizationPolicy {

    boolean can(
            AuthorizationSubject subject, 
            String permission, 
            boolean ownsResource
        );
}
