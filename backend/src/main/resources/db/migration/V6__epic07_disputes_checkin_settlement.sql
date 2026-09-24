-- ==============================================================================
-- Migration: V6__epic07_disputes_checkin_settlement.sql
-- Description: EPIC-07 Domain Migration (Refund constraints, SubOrder statuses,
--              Disputes table, Checkin tokens, Settlement items)
-- ==============================================================================

-- 1. CẬP NHẬT CHECK CONSTRAINT TRÊN BẢNG refunds
-- Cho phép: CUSTOMER_REQUEST, VENDOR_FAULT, ADMIN_OVERRIDE, CUSTOMER_CANCEL, WEATHER, DISPUTE, COMPENSATION
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'refunds'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%reason%'
    ) LOOP
        EXECUTE 'ALTER TABLE refunds DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
    END LOOP;
END $$;

ALTER TABLE refunds DROP CONSTRAINT IF EXISTS refunds_reason_check;
ALTER TABLE refunds DROP CONSTRAINT IF EXISTS chk_refunds_reason;

ALTER TABLE refunds ADD CONSTRAINT chk_refunds_reason CHECK (
    reason IN (
        'CUSTOMER_REQUEST',
        'VENDOR_FAULT',
        'ADMIN_OVERRIDE',
        'CUSTOMER_CANCEL',
        'WEATHER',
        'DISPUTE',
        'COMPENSATION'
    )
);

-- 2. CẬP NHẬT CHECK CONSTRAINT TRÊN BẢNG sub_orders
-- Cho phép: PENDING, CONFIRMED, REJECTED, COMPLETED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED, CHECKED_IN, IN_PROGRESS
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'sub_orders'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%status%'
    ) LOOP
        EXECUTE 'ALTER TABLE sub_orders DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
    END LOOP;
END $$;

ALTER TABLE sub_orders DROP CONSTRAINT IF EXISTS sub_orders_status_check;
ALTER TABLE sub_orders DROP CONSTRAINT IF EXISTS chk_sub_orders_status;

ALTER TABLE sub_orders ADD CONSTRAINT chk_sub_orders_status CHECK (
    status IN (
        'PENDING',
        'CONFIRMED',
        'REJECTED',
        'COMPLETED',
        'CANCELLED',
        'REFUNDED',
        'PARTIALLY_REFUNDED',
        'CHECKED_IN',
        'IN_PROGRESS'
    )
);

-- 3. XÓA BẢNG disputes & dispute_attachments CŨ TỪ SPRINT 1 (NẾU CÓ)
DROP TABLE IF EXISTS dispute_attachments CASCADE;
DROP TABLE IF EXISTS disputes CASCADE;

-- 4. TẠO BẢNG disputes CHUẨN THEO EPIC-07 R2
CREATE TABLE disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID,
    sub_order_id UUID NOT NULL REFERENCES sub_orders(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    evidence_urls TEXT,
    status VARCHAR(50) NOT NULL,
    refund_percentage NUMERIC(5,2),
    admin_note TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_disputes_reason CHECK (
        reason IN (
            'SERVICE_NOT_AS_DESCRIBED',
            'VENDOR_NO_SHOW',
            'SAFETY_CONCERN',
            'PAYMENT_ISSUE',
            'OTHER'
        )
    ),
    CONSTRAINT chk_disputes_status CHECK (
        status IN (
            'OPEN',
            'UNDER_REVIEW',
            'RESOLVED_REFUND',
            'RESOLVED_REJECTED',
            'RESOLVED_PARTIAL'
        )
    ),
    CONSTRAINT chk_disputes_refund_percentage CHECK (
        refund_percentage IS NULL OR (refund_percentage >= 0 AND refund_percentage <= 100)
    )
);

-- Chỉ mục hỗ trợ truy vấn & Chống khiếu nại trùng lặp trên cùng SubOrder
CREATE UNIQUE INDEX idx_disputes_active_sub_order 
    ON disputes(sub_order_id) 
    WHERE status IN ('OPEN', 'UNDER_REVIEW');

CREATE INDEX idx_disputes_sub_order_id ON disputes(sub_order_id);
CREATE INDEX idx_disputes_customer_id ON disputes(customer_id);
CREATE INDEX idx_disputes_order_id ON disputes(order_id);
CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_disputes_created_at ON disputes(created_at DESC);

-- 5. TẠO BẢNG checkin_tokens CHO MILESTONE 3 (EPIC-07 R3)
CREATE TABLE IF NOT EXISTS checkin_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_order_id UUID NOT NULL REFERENCES sub_orders(id),
    qr_token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    used_by_vendor_staff_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_checkin_tokens_hash ON checkin_tokens(qr_token_hash);
CREATE INDEX IF NOT EXISTS idx_checkin_tokens_sub_order ON checkin_tokens(sub_order_id);

-- 6. CẬP NHẬT CHECK CONSTRAINT VÀ LINE ITEMS CHO BẢNG settlements (EPIC-07 R4)
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'settlements'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%status%'
    ) LOOP
        EXECUTE 'ALTER TABLE settlements DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
    END LOOP;
END $$;

ALTER TABLE settlements DROP CONSTRAINT IF EXISTS settlements_status_check;
ALTER TABLE settlements DROP CONSTRAINT IF EXISTS chk_settlements_status;
ALTER TABLE settlements ADD CONSTRAINT chk_settlements_status 
    CHECK (status IN ('DRAFT', 'FINALIZED', 'PAID', 'PENDING'));

CREATE UNIQUE INDEX IF NOT EXISTS idx_settlements_vendor_period 
    ON settlements(vendor_id, period_start, period_end);

CREATE TABLE IF NOT EXISTS settlement_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_id UUID NOT NULL REFERENCES settlements(id) ON DELETE CASCADE,
    sub_order_id UUID NOT NULL REFERENCES sub_orders(id),
    gross_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    refund_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    commission_rate NUMERIC(5,4) NOT NULL DEFAULT 0,
    commission_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    excluded_reason VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_settlement_line_items_exclusion CHECK (
        excluded_reason IS NULL OR excluded_reason IN ('FULL_REFUND', 'ACTIVE_DISPUTE', 'NOT_ELIGIBLE')
    )
);

CREATE INDEX IF NOT EXISTS idx_settlement_line_items_settlement ON settlement_line_items(settlement_id);
CREATE INDEX IF NOT EXISTS idx_settlement_line_items_sub_order ON settlement_line_items(sub_order_id);

