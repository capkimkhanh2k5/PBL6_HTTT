package com.danasport.backend.authorization.domain.policy;

import java.util.Set;

import com.danasport.backend.authorization.domain.model.AuthorizationSubject;

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
