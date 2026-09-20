package com.danasea.backend.modules.booking.domain.models;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InventoryLockItem {
    private final UUID slotId;
    private final int quantity;
    private final int maxCapacity;

    public static InventoryLockItem of(UUID slotId, int quantity, int maxCapacity) {
        if (slotId == null) {
            throw new IllegalArgumentException("slotId cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (maxCapacity < 0) {
            throw new IllegalArgumentException("maxCapacity cannot be negative");
        }
        return new InventoryLockItem(slotId, quantity, maxCapacity);
    }

    public static InventoryLockItem of(UUID slotId, int quantity) {
        return of(slotId, quantity, Integer.MAX_VALUE);
    }
}
