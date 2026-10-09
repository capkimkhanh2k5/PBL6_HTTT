ALTER TABLE payments ADD COLUMN paid_at TIMESTAMPTZ;
ALTER TABLE sub_orders ADD COLUMN cancellation_reason VARCHAR(30);
-- Historical event times were not recorded. Preserve a best available estimate.
UPDATE payments SET paid_at = COALESCE(updated_at, created_at)
WHERE status IN ('SUCCESS', 'REFUNDED');
UPDATE sub_orders SET cancellation_reason = 'VENDOR_FAULT' WHERE status = 'REJECTED';
CREATE INDEX idx_payments_paid_at ON payments(paid_at) WHERE paid_at IS NOT NULL;
CREATE INDEX idx_refunds_processed_at ON refunds(processed_at) WHERE status = 'PROCESSED';
