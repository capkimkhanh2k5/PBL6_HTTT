package com.danasea.backend.modules.operation.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseDomainModel {
    private UUID subOrderId;
    private UUID customerId;
    private UUID vendorId;
    private UUID serviceId;
    private Short rating;
    private String comment;
    private String images;
    private String vendorReply;
    private OffsetDateTime vendorRepliedAt;
    private Boolean isFlagged;
    private Boolean isVisible;
    private String flagReason;
}
