package com.danasea.backend.modules.booking.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_item_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItemAllocationJpaEntity extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_item_id", nullable = false)
    private BookingItemJpaEntity bookingItem;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "unit_number", nullable = false)
    private Integer unitNumber;

    @Column(name = "allocated_seats", nullable = false)
    private Integer allocatedSeats;

    @Column(name = "is_private_lock", nullable = false)
    private Boolean isPrivateLock;
}
