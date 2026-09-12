package com.danasea.backend.security.authorization.domain.policies;

import java.util.Set;

import com.danasea.backend.security.authorization.domain.models.AuthorizationSubject;

public class DefaultAuthorizationPolicy implements AuthorizationPolicy {

    @Override
    public boolean can(
            AuthorizationSubject subject, 
            String permission, 
            boolean ownsResource
        ) {
            if (subject.roles().contains("ADMIN")) {
                return true;
            }

            if(subject.permissions().contains(permission)) {
                return true;
            }

            return ownsResource && allowsOwnerAction(permission);
    }   

    private boolean allowsOwnerAction(String permission) {
        return Set.of(
            "PROFILE_READ",
            "PROFILE_UPDATE",
            "ORDER_READ",
            "ORDER_UPDATE"
        ).contains(permission);
    }
    
}
