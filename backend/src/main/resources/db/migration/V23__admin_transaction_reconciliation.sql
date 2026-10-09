-- V21/V22 are reserved by the AI/reporting worktrees.
ALTER TABLE payments ADD COLUMN reconciliation_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN reconciliation_next_attempt_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE payments ADD COLUMN last_reconciled_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE audit_logs ALTER COLUMN metadata TYPE TEXT;

CREATE INDEX idx_payments_reconciliation_pending
    ON payments (reconciliation_next_attempt_at, created_at, id)
    WHERE status = 'PENDING';

-- Recover the money status from recorded successful gateway payments.
UPDATE master_orders o SET payment_status = 'PAID'
WHERE o.payment_status = 'UNPAID'
  AND EXISTS (SELECT 1 FROM payments p WHERE p.master_order_id = o.id AND p.status = 'SUCCESS');

UPDATE master_orders o SET payment_status = 'REFUNDED'
WHERE EXISTS (SELECT 1 FROM payments p WHERE p.master_order_id = o.id AND p.status = 'REFUNDED');
