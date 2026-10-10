-- Migration: Add notification read status and read timestamp tracking
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read_at TIMESTAMP(6) WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_notifications_user_is_read ON notifications(user_id, is_read);

ALTER TABLE notifications ADD COLUMN idempotency_key VARCHAR(160);
CREATE UNIQUE INDEX uk_notifications_idempotency_key ON notifications(idempotency_key);
CREATE INDEX idx_service_slots_departure ON service_slots(date, start_time);
