-- V18__service_options_and_shared_capacity_units.sql
-- 1. Create service_options table
CREATE TABLE IF NOT EXISTS service_options (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    option_type VARCHAR(50) NOT NULL CHECK (option_type IN ('SHARED', 'PRIVATE')),
    pricing_unit VARCHAR(50) NOT NULL CHECK (pricing_unit IN ('PER_PERSON', 'PER_PACKAGE')),
    price DECIMAL(15, 2) NOT NULL,
    max_pax_per_package INTEGER,
    benefits TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_service_options_service_id ON service_options(service_id);
CREATE INDEX IF NOT EXISTS idx_service_options_service_id_status ON service_options(service_id, status);

-- 2. Add inventory_type to service_slots
ALTER TABLE service_slots 
ADD COLUMN IF NOT EXISTS inventory_type VARCHAR(50) NOT NULL DEFAULT 'PERSON_LIMIT' 
CHECK (inventory_type IN ('PERSON_LIMIT', 'SHARED_CAPACITY_UNITS'));

-- 3. Create service_slot_units table
CREATE TABLE IF NOT EXISTS service_slot_units (
    id UUID PRIMARY KEY,
    slot_id UUID NOT NULL REFERENCES service_slots(id) ON DELETE CASCADE,
    unit_number INTEGER NOT NULL,
    capacity INTEGER NOT NULL,
    booked_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_slot_unit UNIQUE (slot_id, unit_number)
);

CREATE INDEX IF NOT EXISTS idx_service_slot_units_slot_id ON service_slot_units(slot_id);

-- 4. Add option columns to booking_items and align vendor_id reference
ALTER TABLE booking_items 
DROP CONSTRAINT IF EXISTS booking_items_vendor_id_fkey,
ADD COLUMN IF NOT EXISTS option_id UUID REFERENCES service_options(id),
ADD COLUMN IF NOT EXISTS pricing_unit VARCHAR(50),
ADD COLUMN IF NOT EXISTS participants_count INTEGER;

-- 5. Create booking_item_allocations table
CREATE TABLE IF NOT EXISTS booking_item_allocations (
    id UUID PRIMARY KEY,
    booking_item_id UUID NOT NULL REFERENCES booking_items(id) ON DELETE CASCADE,
    slot_id UUID NOT NULL REFERENCES service_slots(id),
    unit_number INTEGER NOT NULL,
    allocated_seats INTEGER NOT NULL,
    is_private_lock BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_booking_item_alloc_item_id ON booking_item_allocations(booking_item_id);
CREATE INDEX IF NOT EXISTS idx_booking_item_alloc_slot_id ON booking_item_allocations(slot_id);

-- 6. Seed default SHARED option for all existing services to guarantee backward compatibility
INSERT INTO service_options (
    id,
    service_id,
    name,
    option_type,
    pricing_unit,
    price,
    max_pax_per_package,
    benefits,
    status,
    created_at,
    updated_at
)
SELECT 
    gen_random_uuid(),
    s.id,
    'Tour ghép',
    'SHARED',
    'PER_PERSON',
    COALESCE(s.price, 0),
    NULL,
    'Trải nghiệm tour ghép tiêu chuẩn đã bao gồm trong dịch vụ',
    'ACTIVE',
    NOW(),
    NOW()
FROM services s
WHERE NOT EXISTS (
    SELECT 1 FROM service_options o WHERE o.service_id = s.id
);
