ALTER TABLE settlements ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- A sub-order may only have one active QR token at a time. Used tokens remain as an audit trail.
CREATE UNIQUE INDEX IF NOT EXISTS uk_checkin_tokens_active_sub_order
    ON checkin_tokens(sub_order_id) WHERE used_at IS NULL;
