package com.danasea.backend.authorization.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.authorization.application.port.ResourceOwnershipPort;

@Component
public class DenyByDefaultOwnershipAdapter implements ResourceOwnershipPort {

    @Override
    public boolean isOwner(
            UUID userId,
            String resourceType,
            UUID resourceId
    ) {
        return false;
    }
}