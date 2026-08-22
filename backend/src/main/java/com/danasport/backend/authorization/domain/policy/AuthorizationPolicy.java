package com.danasport.backend.authorization.domain.policy;

import com.danasport.backend.authorization.domain.model.AuthorizationSubject;

public interface AuthorizationPolicy {

    boolean can(
            AuthorizationSubject subject, 
            String permission, 
            boolean ownsResource
        );
}
