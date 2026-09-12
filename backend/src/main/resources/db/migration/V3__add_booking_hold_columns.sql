-- V3: Add hold_expires_at to bookings and slot_id to booking_items
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS hold_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE booking_items ADD COLUMN IF NOT EXISTS slot_id UUID REFERENCES service_slots(id);

CREATE INDEX IF NOT EXISTS idx_bookings_hold_expires_at ON bookings(hold_expires_at) WHERE status = 'HOLD';
CREATE INDEX IF NOT EXISTS idx_booking_items_slot_id ON booking_items(slot_id);
