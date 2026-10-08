ALTER TABLE booking_items ADD COLUMN capacity_released BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE booking_items bi SET capacity_released = TRUE
WHERE EXISTS (SELECT 1 FROM bookings b WHERE b.id = bi.booking_id AND b.status IN ('CANCELLED', 'EXPIRED'))
   OR EXISTS (SELECT 1 FROM sub_orders s WHERE s.booking_item_id = bi.id
              AND s.status IN ('CANCELLED', 'REJECTED', 'REFUNDED', 'PARTIALLY_REFUNDED'));

-- Reconcile shared-unit counters with confirmed allocations, excluding released items.
UPDATE service_slot_units u SET booked_count = (
    SELECT COALESCE(SUM(a.allocated_seats), 0)
    FROM booking_item_allocations a JOIN booking_items bi ON bi.id = a.booking_item_id
    JOIN bookings b ON b.id = bi.booking_id
    WHERE a.slot_id = u.slot_id AND a.unit_number = u.unit_number
      AND b.status IN ('CONFIRMED', 'COMPLETED') AND NOT bi.capacity_released
);
UPDATE service_slots s SET booked_count = (
    SELECT COALESCE(SUM(u.booked_count), 0) FROM service_slot_units u WHERE u.slot_id = s.id
) WHERE s.inventory_type = 'SHARED_CAPACITY_UNITS';

ALTER TABLE service_slots ADD CONSTRAINT uq_service_departure UNIQUE (service_id, date, start_time);
