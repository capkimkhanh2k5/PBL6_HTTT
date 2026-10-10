-- V35__sub_order_waiver_acceptance.sql
-- Sub-order waiver acceptance, policy management, and order snapshot

-- 1. Extend services table with waiver requirement policy, versioning and full-text content
ALTER TABLE services ALTER COLUMN waiver_content TYPE TEXT;
ALTER TABLE services ADD COLUMN IF NOT EXISTS waiver_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE services ADD COLUMN IF NOT EXISTS waiver_version INTEGER NOT NULL DEFAULT 1;

-- 2. Extend sub_orders table with snapshot and acceptance proof
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS waiver_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS waiver_version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS waiver_content TEXT;
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS waiver_content_en TEXT;
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS waiver_accepted_by UUID REFERENCES users(id);
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS waiver_accepted_language VARCHAR(10);
ALTER TABLE sub_orders ADD COLUMN IF NOT EXISTS waiver_accepted_content TEXT;

-- Index for querying unaccepted required waivers by master order
CREATE INDEX IF NOT EXISTS idx_sub_orders_master_order_waiver
    ON sub_orders(master_order_id, waiver_required, waiver_accepted);

-- 3. Backfill snapshot for PENDING sub_orders under PENDING_PAYMENT master orders
UPDATE sub_orders so
SET
    waiver_required = COALESCE(s.waiver_required, FALSE),
    waiver_version = COALESCE(s.waiver_version, 1),
    waiver_content = s.waiver_content,
    waiver_content_en = s.waiver_content_en
FROM services s, master_orders mo
WHERE so.service_id = s.id
  AND so.master_order_id = mo.id
  AND mo.status = 'PENDING_PAYMENT'
  AND so.status = 'PENDING';
