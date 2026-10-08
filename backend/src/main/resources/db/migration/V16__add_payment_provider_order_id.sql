-- V16: Add provider_order_id to payments table for PayPal Order ID tracking
ALTER TABLE payments
    ADD COLUMN provider_order_id VARCHAR(100);

-- Partial unique index to prevent duplicate intents with the same external provider order ID
CREATE UNIQUE INDEX uq_payments_provider_order_id
    ON payments (provider, provider_order_id)
    WHERE provider_order_id IS NOT NULL;
