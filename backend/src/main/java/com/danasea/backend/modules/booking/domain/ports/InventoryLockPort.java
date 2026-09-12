package com.danasea.backend.modules.booking.domain.ports;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.InventoryLockItem;

public interface InventoryLockPort {

    /**
     * Attempts to atomically lock inventory across all requested slot items using a single batch Lua script.
     * If any slot has insufficient capacity, no locks are acquired and InsufficientInventoryException is thrown.
     *
     * @param holdId identifier of the hold/booking
     * @param items list of slot items and quantities to hold
     * @param ttl time-to-live for the temporary hold
     */
    void acquireHolds(UUID holdId, List<InventoryLockItem> items, Duration ttl);

    /**
     * Releases held slots for a given holdId, freeing inventory back into the pool.
     *
     * @param holdId identifier of the hold/booking
     * @param items list of slot items to release
     */
    void releaseHolds(UUID holdId, List<InventoryLockItem> items);
}
