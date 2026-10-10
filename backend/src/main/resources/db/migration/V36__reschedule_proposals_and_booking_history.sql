-- V36__reschedule_proposals_and_booking_history.sql

-- 1. Thêm các cột theo dõi đổi lịch cho bảng sub_orders
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS reschedule_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS rescheduled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS original_slot_id UUID REFERENCES service_slots(id);

-- 2. Bảng lưu trữ đề xuất đổi ca từ vendor
CREATE TABLE IF NOT EXISTS sub_order_reschedule_proposals (
    id UUID PRIMARY KEY,
    sub_order_id UUID NOT NULL REFERENCES sub_orders(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    proposed_slot_id UUID NOT NULL REFERENCES service_slots(id),
    reason TEXT NOT NULL,
    reason_type VARCHAR(50) NOT NULL CHECK (reason_type IN ('OPERATIONAL', 'WEATHER')),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'SUPERSEDED')),
    proposal_version BIGINT NOT NULL DEFAULT 0,
    notification_sent_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reschedule_proposals_sub_order ON sub_order_reschedule_proposals(sub_order_id, status);
CREATE INDEX IF NOT EXISTS idx_reschedule_proposals_vendor ON sub_order_reschedule_proposals(vendor_id);
CREATE INDEX IF NOT EXISTS idx_reschedule_proposals_slot ON sub_order_reschedule_proposals(proposed_slot_id);

-- 3. Bảng lưu trữ lịch sử chuyển ca đối soát và audit
CREATE TABLE IF NOT EXISTS sub_order_reschedule_history (
    id UUID PRIMARY KEY,
    sub_order_id UUID NOT NULL REFERENCES sub_orders(id) ON DELETE CASCADE,
    booking_item_id UUID NOT NULL REFERENCES booking_items(id),
    proposal_id UUID REFERENCES sub_order_reschedule_proposals(id),
    from_slot_id UUID NOT NULL REFERENCES service_slots(id),
    to_slot_id UUID NOT NULL REFERENCES service_slots(id),
    from_booking_date DATE NOT NULL,
    from_booking_time TIME NOT NULL,
    to_booking_date DATE NOT NULL,
    to_booking_time TIME NOT NULL,
    performed_by UUID NOT NULL REFERENCES users(id),
    reason_type VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    payload_hash VARCHAR(64),
    reschedule_version BIGINT NOT NULL DEFAULT 1,
    notification_sent_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reschedule_idempotency UNIQUE (sub_order_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_reschedule_history_sub_order ON sub_order_reschedule_history(sub_order_id);
CREATE INDEX IF NOT EXISTS idx_reschedule_history_to_slot ON sub_order_reschedule_history(to_slot_id);


-- Freeze the purchased package limit and the customer-approved split policy.
ALTER TABLE booking_items ADD COLUMN IF NOT EXISTS max_pax_per_package INTEGER;
ALTER TABLE booking_items ADD COLUMN IF NOT EXISTS allow_split BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE booking_items bi
SET max_pax_per_package = GREATEST(
    COALESCE(o.max_pax_per_package, 0),
    (COALESCE(bi.participants_count, 0) + GREATEST(bi.quantity, 1) - 1) / GREATEST(bi.quantity, 1),
    COALESCE((SELECT MAX(a.allocated_seats) FROM booking_item_allocations a
              WHERE a.booking_item_id = bi.id AND a.is_private_lock = TRUE), 0), 1)
FROM service_options o
WHERE bi.option_id = o.id AND bi.pricing_unit = 'PER_PACKAGE';
UPDATE booking_items bi
SET allow_split = TRUE
WHERE bi.pricing_unit = 'PER_PERSON'
  AND (SELECT COUNT(*) FROM booking_item_allocations a WHERE a.booking_item_id = bi.id) > 1;
ALTER TABLE booking_items ADD CONSTRAINT ck_booking_package_limit
    CHECK (max_pax_per_package IS NULL OR max_pax_per_package > 0);
ALTER TABLE sub_order_reschedule_history ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE sub_order_reschedule_history ADD COLUMN IF NOT EXISTS reschedule_version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sub_order_reschedule_history ADD COLUMN IF NOT EXISTS notification_sent_at TIMESTAMPTZ;
ALTER TABLE sub_order_reschedule_proposals ADD COLUMN IF NOT EXISTS notification_sent_at TIMESTAMPTZ;
CREATE INDEX idx_reschedule_history_pending_notification
    ON sub_order_reschedule_history(created_at) WHERE notification_sent_at IS NULL;
CREATE INDEX idx_reschedule_proposals_pending_notification
    ON sub_order_reschedule_proposals(created_at) WHERE notification_sent_at IS NULL;
