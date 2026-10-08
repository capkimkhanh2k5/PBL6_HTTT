-- EPIC-04 / P0: Add retry count, error tracking and provider transaction reference to refunds
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS last_error TEXT;
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS provider_transaction_id VARCHAR(255);
