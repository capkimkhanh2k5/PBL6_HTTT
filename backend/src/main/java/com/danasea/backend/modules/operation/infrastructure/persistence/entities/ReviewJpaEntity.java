package com.danasea.backend.modules.operation.infrastructure.persistence.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "reviews")
public class ReviewJpaEntity extends BaseJpaEntity {

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

}
