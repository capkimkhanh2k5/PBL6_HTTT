package com.danasea.backend.modules.operation.infrastructure.persistence.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "reviews")
public class ReviewJpaEntity extends BaseJpaEntity {

    @Column(name = "sub_order_id", nullable = false)
    private UUID subOrderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "rating", nullable = false)
    private Short rating;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "images", length = 2000)
    private String images;

    @Column(name = "vendor_reply", length = 2000)
    private String vendorReply;

    @Column(name = "vendor_replied_at")
    private OffsetDateTime vendorRepliedAt;

    @Column(name = "is_flagged")
    @Builder.Default
    private Boolean isFlagged = false;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean isVisible = true;

    @Column(name = "flag_reason")
    private String flagReason;

}
