-- ============================================================================
-- V521: Split Remaining Colliding Entity Table Mappings
-- Separates the remaining 3 JPA entity pairs that shared physical tables
-- with incompatible schemas:
--   1. com.manacommunity.api.model.SmartMeter -> community_smart_meters
--      (retains manacommunity.smart_meters for iot.metering.IotSmartMeter + V146 FKs)
--   2. com.manacommunity.api.schoolbus.entity.BusTripLog -> school_bus_attendance_log
--      (retains manacommunity.school_bus_trip_log for mobile.schoolbus.MobileBusTripLog)
--   3. com.manacommunity.api.platform.dpdp.DataRightsRequest -> dpdp_data_rights_requests
--      (retains manacommunity.data_rights_requests for privacy.DataRightsRequest)
-- ============================================================================

-- 1. Dedicated table for com.manacommunity.api.model.SmartMeter
CREATE TABLE IF NOT EXISTS manacommunity.community_smart_meters (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    flat_number VARCHAR(20) NOT NULL,
    meter_type VARCHAR(20) NOT NULL,
    meter_serial VARCHAR(50) NOT NULL,
    location VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_reading NUMERIC(14,2),
    last_reading_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_community_smart_meter_serial UNIQUE (meter_serial)
);

CREATE INDEX IF NOT EXISTS idx_comm_smart_meters_community_id
    ON manacommunity.community_smart_meters (community_id);
CREATE INDEX IF NOT EXISTS idx_comm_smart_meters_flat
    ON manacommunity.community_smart_meters (community_id, flat_number);

-- Migrate any pure legacy api.model.SmartMeter rows if present without disturbing IoT rows
DO $$
BEGIN
    IF to_regclass('manacommunity.smart_meters') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='smart_meters' AND column_name='flat_number')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='smart_meters' AND column_name='meter_serial') THEN
        INSERT INTO manacommunity.community_smart_meters (
            community_id, flat_number, meter_type, meter_serial, location, active, last_reading, last_reading_date, created_at
        )
        SELECT
            s.community_id,
            s.flat_number,
            s.meter_type,
            s.meter_serial,
            s.location,
            COALESCE(s.active, TRUE),
            ROUND(s.last_reading::numeric, 2),
            s.last_reading_date,
            COALESCE(s.created_at, NOW())
        FROM manacommunity.smart_meters s
        WHERE s.meter_serial_number IS NULL
          AND s.flat_number IS NOT NULL
          AND s.meter_serial IS NOT NULL
          AND char_length(s.flat_number) <= 20
          AND char_length(s.meter_serial) <= 50
          AND s.meter_type IN ('ELECTRICITY', 'WATER', 'GAS')
        ON CONFLICT (meter_serial) DO NOTHING;
    END IF;
END $$;

-- 2. Dedicated table for com.manacommunity.api.schoolbus.entity.BusTripLog
CREATE TABLE IF NOT EXISTS manacommunity.school_bus_attendance_log (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL,
    trip_date DATE NOT NULL,
    pickup_time TIMESTAMP,
    drop_time TIMESTAMP,
    attendance VARCHAR(20) DEFAULT 'PRESENT',
    bus_id BIGINT,
    driver_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_sb_att_log_child_id
    ON manacommunity.school_bus_attendance_log (child_id);
CREATE INDEX IF NOT EXISTS idx_sb_att_log_bus_id
    ON manacommunity.school_bus_attendance_log (bus_id);
CREATE INDEX IF NOT EXISTS idx_sb_att_log_trip_date
    ON manacommunity.school_bus_attendance_log (trip_date);

DO $$
BEGIN
    IF to_regclass('manacommunity.school_bus_trip_log') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='school_bus_trip_log' AND column_name='attendance') THEN
        INSERT INTO manacommunity.school_bus_attendance_log (
            child_id, trip_date, pickup_time, drop_time, attendance, bus_id, driver_name, created_at, updated_at, created_by, updated_by
        )
        SELECT
            t.child_id,
            t.trip_date,
            t.pickup_time,
            t.drop_time,
            COALESCE(t.attendance, 'PRESENT'),
            t.bus_id,
            t.driver_name,
            COALESCE(t.created_at, CURRENT_TIMESTAMP),
            COALESCE(t.updated_at, COALESCE(t.created_at, CURRENT_TIMESTAMP)),
            t.created_by,
            t.updated_by
        FROM manacommunity.school_bus_trip_log t
        WHERE t.direction IS NULL
          AND t.child_id IS NOT NULL
          AND t.trip_date IS NOT NULL;
    END IF;
END $$;

-- 3. Dedicated table for com.manacommunity.api.platform.dpdp.DataRightsRequest
CREATE TABLE IF NOT EXISTS manacommunity.dpdp_data_rights_requests (
    id BIGSERIAL PRIMARY KEY,
    resident_id BIGINT,
    community_id BIGINT,
    request_type VARCHAR(50),
    status VARCHAR(30) DEFAULT 'PENDING',
    reason TEXT,
    requested_at TIMESTAMPTZ DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    resolved_by BIGINT,
    resolution_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dpdp_drr_resident
    ON manacommunity.dpdp_data_rights_requests (resident_id, status);
CREATE INDEX IF NOT EXISTS idx_dpdp_drr_community
    ON manacommunity.dpdp_data_rights_requests (community_id, status);
CREATE INDEX IF NOT EXISTS idx_dpdp_drr_status
    ON manacommunity.dpdp_data_rights_requests (status);

DO $$
BEGIN
    IF to_regclass('manacommunity.data_rights_requests') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='data_rights_requests' AND column_name='resident_id') THEN
        INSERT INTO manacommunity.dpdp_data_rights_requests (
            resident_id, community_id, request_type, status, reason, requested_at, resolved_at, resolved_by, resolution_notes, created_at
        )
        SELECT
            d.resident_id,
            d.community_id,
            d.request_type,
            COALESCE(d.status, 'PENDING'),
            d.reason,
            COALESCE(d.requested_at, NOW()),
            d.resolved_at,
            d.resolved_by,
            d.resolution_notes,
            COALESCE(d.created_at, NOW())
        FROM manacommunity.data_rights_requests d
        WHERE d.resident_id IS NOT NULL
          AND (d.user_id IS NULL OR d.user_email IS NULL OR d.right_type IS NULL);
    END IF;
END $$;
