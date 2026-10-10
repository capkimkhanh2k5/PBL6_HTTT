-- Migration V21: Review enhancements for visibility, flagging, and performance
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS is_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS flag_reason VARCHAR(255);

ALTER TABLE reviews ALTER COLUMN comment TYPE VARCHAR(2000);
ALTER TABLE reviews ALTER COLUMN images TYPE TEXT;
ALTER TABLE reviews ALTER COLUMN vendor_reply TYPE VARCHAR(2000);

CREATE INDEX IF NOT EXISTS idx_reviews_service_id_visible ON reviews(service_id, is_visible);
CREATE INDEX IF NOT EXISTS idx_reviews_vendor_id_visible ON reviews(vendor_id, is_visible);
CREATE INDEX IF NOT EXISTS idx_reviews_sub_order_id ON reviews(sub_order_id);

ALTER TABLE reviews ADD COLUMN moderation_note VARCHAR(255);
ALTER TABLE reviews ADD CONSTRAINT uq_reviews_sub_order UNIQUE (sub_order_id);
ALTER TABLE reviews ADD CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5);
