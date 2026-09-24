-- Link the legacy booking hold to a single order and make financial writes idempotent.
ALTER TABLE master_orders ADD COLUMN IF NOT EXISTS booking_id UUID REFERENCES bookings(id);
ALTER TABLE master_orders ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS uk_master_orders_booking_id
    ON master_orders(booking_id) WHERE booking_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_master_orders_customer_idempotency
    ON master_orders(customer_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS booking_item_id UUID REFERENCES booking_items(id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sub_orders_booking_item_id
    ON sub_orders(booking_item_id) WHERE booking_item_id IS NOT NULL;

ALTER TABLE payments ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS webhook_event_id VARCHAR(150);
ALTER TABLE payments ALTER COLUMN raw_webhook_payload TYPE TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_order_idempotency
    ON payments(master_order_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_provider_event
    ON payments(provider, webhook_event_id) WHERE webhook_event_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_provider_transaction
    ON payments(provider, provider_transaction_id) WHERE provider_transaction_id IS NOT NULL;

ALTER TABLE refunds ADD COLUMN IF NOT EXISTS requested_by UUID REFERENCES users(id);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS provider_refund_id VARCHAR(150);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS provider VARCHAR(50);
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS webhook_event_id VARCHAR(150);

CREATE UNIQUE INDEX IF NOT EXISTS uk_refunds_sub_order_idempotency
    ON refunds(sub_order_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_refunds_provider_refund_id
    ON refunds(provider_refund_id) WHERE provider_refund_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_refunds_provider_event
    ON refunds(provider, webhook_event_id) WHERE provider IS NOT NULL AND webhook_event_id IS NOT NULL;
