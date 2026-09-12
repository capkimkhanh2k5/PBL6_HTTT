package com.danasea.backend.security.authorization.application.ports;

import java.util.UUID;

public interface ResourceOwnershipPort {
    
    boolean isOwner(UUID userId, String resourceType, UUID resourceId);
}
