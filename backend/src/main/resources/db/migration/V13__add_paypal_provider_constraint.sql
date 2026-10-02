-- EPIC-04: Support PAYPAL payment provider
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_provider_check;
ALTER TABLE payments ADD CONSTRAINT payments_provider_check CHECK (provider IN ('VNPAY', 'MOMO', 'SEPAY', 'PAYPAL'));
