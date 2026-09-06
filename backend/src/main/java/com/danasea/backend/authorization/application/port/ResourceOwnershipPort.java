package com.danasea.backend.authorization.application.port;

import java.util.UUID;

public interface ResourceOwnershipPort {
    
    boolean isOwner(UUID userId, String resourceType, UUID resourceId);
}
