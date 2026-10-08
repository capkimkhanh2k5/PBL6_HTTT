-- EPIC-04 / P0: Add payment intent URL, QR URL and expiry timestamp for idempotency recovery
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_url TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS qr_code_url TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE;
