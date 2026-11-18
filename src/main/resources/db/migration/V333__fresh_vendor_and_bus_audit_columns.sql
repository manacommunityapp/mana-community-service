-- V333: Ensure audit and operational columns exist for fresh vendor capabilities and school bus trip logs
-- Resolves SchemaManagementException during Hibernate schema validation for entities extending BaseAuditEntity

-- 1. Fresh Vendor Capabilities
ALTER TABLE manacommunity.fresh_vendor_capability
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- 2. Fresh Vendor Staff
ALTER TABLE manacommunity.fresh_vendor_staff
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- 3. Fresh Vendor Service Area
ALTER TABLE manacommunity.fresh_vendor_service_area
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

-- 4. School Bus Trip Log
ALTER TABLE manacommunity.school_bus_trip_log
    ADD COLUMN IF NOT EXISTS pickup_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS drop_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS attendance VARCHAR(20) DEFAULT 'PRESENT',
    ADD COLUMN IF NOT EXISTS driver_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();
