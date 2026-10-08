-- Persist the original provider money/reference and operation identities before remote calls.
ALTER TABLE payments
    ADD COLUMN provider_amount NUMERIC(20,2),
    ADD COLUMN provider_currency VARCHAR(3),
    ADD COLUMN provider_transaction_date VARCHAR(14),
    ADD COLUMN capture_request_id VARCHAR(100),
    ADD COLUMN capture_requested_at TIMESTAMPTZ,
    ADD COLUMN last_error TEXT;

ALTER TABLE refunds
    ADD COLUMN payment_id UUID REFERENCES payments(id),
    ADD COLUMN provider_amount NUMERIC(20,2),
    ADD COLUMN provider_currency VARCHAR(3),
    ADD COLUMN gateway_request_id VARCHAR(100),
    ADD COLUMN gateway_requested_at TIMESTAMPTZ,
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD COLUMN verification_attempts INTEGER NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uq_payment_capture_request ON payments(capture_request_id) WHERE capture_request_id IS NOT NULL;
CREATE UNIQUE INDEX uq_refund_gateway_request ON refunds(gateway_request_id) WHERE gateway_request_id IS NOT NULL;
