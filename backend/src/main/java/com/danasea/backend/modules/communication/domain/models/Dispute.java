package com.danasea.backend.modules.communication.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Dispute extends BaseDomainModel {
    private UUID subOrderId;
    private UUID raisedBy;
    private String category;
    private String description;
    private DisputeStatus status;
    private String resolutionNote;
    private UUID resolvedBy;
    private OffsetDateTime resolvedAt;
}
