package com.danasea.backend.modules.booking.domain.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.InventoryType;
import com.danasea.backend.modules.service.domain.models.OptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InventoryLockItem {
    private final UUID slotId;
    private final int quantity;
    private final int maxCapacity;
    private final InventoryType inventoryType;
    private final OptionType optionType;
    private final Integer paxPerPackage;
    @Builder.Default
    private final List<UnitLockInfo> units = new ArrayList<>();
    @Builder.Default
    private List<BookingItemAllocation> allocations = new ArrayList<>();

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
        return InventoryLockItem.builder()
                .slotId(slotId)
                .quantity(quantity)
                .maxCapacity(maxCapacity)
                .inventoryType(InventoryType.PERSON_LIMIT)
                .optionType(OptionType.SHARED)
                .paxPerPackage(null)
                .units(new ArrayList<>())
                .allocations(new ArrayList<>())
                .build();
    }

    public static InventoryLockItem of(UUID slotId, int quantity) {
        return of(slotId, quantity, Integer.MAX_VALUE);
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class UnitLockInfo {
        private final int unitNumber;
        private final int capacity;
        private final int bookedCount;
    }
}
