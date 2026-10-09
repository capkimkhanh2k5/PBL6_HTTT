-- EPIC: Discount Management API Enhancements (P2)
-- Add sponsor type, minimum order amount, max discount cap, per-user usage limit, and optional service restriction to discount_codes
ALTER TABLE discount_codes ADD COLUMN IF NOT EXISTS sponsor_type VARCHAR(50) NOT NULL DEFAULT 'PLATFORM';
ALTER TABLE discount_codes ADD COLUMN IF NOT EXISTS min_order_amount NUMERIC(38,2) DEFAULT 0;
ALTER TABLE discount_codes ADD COLUMN IF NOT EXISTS max_discount_amount NUMERIC(38,2);
ALTER TABLE discount_codes ADD COLUMN IF NOT EXISTS max_uses_per_user INTEGER;
ALTER TABLE discount_codes ADD COLUMN IF NOT EXISTS service_id UUID;

-- Add customer_id to discount_redemptions for fast per-user usage checks
ALTER TABLE discount_redemptions ADD COLUMN IF NOT EXISTS customer_id UUID;
CREATE INDEX IF NOT EXISTS idx_discount_redemptions_code_customer ON discount_redemptions(discount_code_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_discount_redemptions_master_order ON discount_redemptions(master_order_id);

-- Add discount breakdown and commission basis to sub_orders
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(38,2) NOT NULL DEFAULT 0;
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS vendor_discount_amount NUMERIC(38,2) NOT NULL DEFAULT 0;
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS platform_discount_amount NUMERIC(38,2) NOT NULL DEFAULT 0;
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS commission_basis_amount NUMERIC(38,2);
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS final_amount NUMERIC(38,2) NOT NULL DEFAULT 0;

-- Backfill final_amount and commission_basis_amount for existing sub_orders
UPDATE sub_orders SET final_amount = subtotal_amount WHERE final_amount = 0 AND subtotal_amount > 0;
UPDATE sub_orders SET commission_basis_amount = subtotal_amount WHERE commission_basis_amount IS NULL;

-- Preserve vendor funding and customer usage from the pre-enhancement schema.
UPDATE discount_codes SET sponsor_type = 'VENDOR' WHERE scope = 'VENDOR';
UPDATE discount_redemptions r SET customer_id = o.customer_id
FROM master_orders o WHERE r.master_order_id = o.id AND r.customer_id IS NULL;
