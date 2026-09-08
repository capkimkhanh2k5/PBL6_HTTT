package com.danasea.backend.modules.service.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RecentlyViewed extends BaseDomainModel {
    private UUID userId;
    private String sessionId;
    private UUID serviceId;
    private OffsetDateTime viewedAt;
}
