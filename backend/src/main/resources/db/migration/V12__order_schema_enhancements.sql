-- EPIC-04: Order & Payment schema enhancements
-- Add payment deadline and 4-state payment status to master_orders
ALTER TABLE master_orders ADD COLUMN IF NOT EXISTS payment_deadline TIMESTAMP WITH TIME ZONE;
ALTER TABLE master_orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID';

-- Add vendor notification timestamp for outbox compensating control to sub_orders
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS vendor_notified_at TIMESTAMP WITH TIME ZONE;
