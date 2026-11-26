-- ============================================================================
-- V523: Mana Mobility Complete Feature Hardening
--   1. P1 Service-Zone Operations: seed default zones per community, driver zone
--      assignment (mobility_driver.service_zone_id), and trip zone linkage.
--   2. P1 Online Payments, Refunds, Receipts & Driver Settlements on
--      mobility_trip and mobility_carpool_booking.
--   3. P1 Journey Automation (recurring slot instances), Geo/Detour Matching,
--      Cross-Community Opt-In, and School Travel Safeguarding / Consent.
-- ============================================================================

-- 1. Seed default service zones for communities lacking active zones
INSERT INTO manacommunity.mobility_service_zone (
    community_id, name, center_lat, center_lng, radius_km, is_active
)
SELECT
    c.id,
    c.name || ' Core Zone',
    CASE
        WHEN LOWER(COALESCE(c.city, '')) LIKE '%bengaluru%' OR LOWER(COALESCE(c.city, '')) LIKE '%bangalore%' THEN 12.9716
        WHEN LOWER(COALESCE(c.city, '')) LIKE '%mumbai%' THEN 19.0760
        WHEN LOWER(COALESCE(c.city, '')) LIKE '%delhi%' OR LOWER(COALESCE(c.city, '')) LIKE '%gurgaon%' OR LOWER(COALESCE(c.city, '')) LIKE '%noida%' THEN 28.6139
        WHEN LOWER(COALESCE(c.city, '')) LIKE '%chennai%' THEN 13.0827
        WHEN LOWER(COALESCE(c.city, '')) LIKE '%pune%' THEN 18.5204
        ELSE 17.4400
    END,
    CASE
        WHEN LOWER(COALESCE(c.city, '')) LIKE '%bengaluru%' OR LOWER(COALESCE(c.city, '')) LIKE '%bangalore%' THEN 77.5946
        WHEN LOWER(COALESCE(c.city, '')) LIKE '%mumbai%' THEN 72.8777
        WHEN LOWER(COALESCE(c.city, '')) LIKE '%delhi%' OR LOWER(COALESCE(c.city, '')) LIKE '%gurgaon%' OR LOWER(COALESCE(c.city, '')) LIKE '%noida%' THEN 77.2090
        WHEN LOWER(COALESCE(c.city, '')) LIKE '%chennai%' THEN 80.2707
        WHEN LOWER(COALESCE(c.city, '')) LIKE '%pune%' THEN 73.8567
        ELSE 78.3489
    END,
    25.00,
    TRUE
FROM manacommunity.community c
WHERE NOT EXISTS (
    SELECT 1 FROM manacommunity.mobility_service_zone z WHERE z.community_id = c.id
);

-- 2. Driver service-zone assignment
ALTER TABLE manacommunity.mobility_driver
    ADD COLUMN IF NOT EXISTS service_zone_id BIGINT REFERENCES manacommunity.mobility_service_zone(id);

CREATE INDEX IF NOT EXISTS idx_mobility_driver_service_zone
    ON manacommunity.mobility_driver (service_zone_id);

-- 3. MobilityTrip payment, refund, receipt & driver settlement columns
ALTER TABLE manacommunity.mobility_trip
    ADD COLUMN IF NOT EXISTS service_zone_id BIGINT REFERENCES manacommunity.mobility_service_zone(id),
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(25) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS payment_order_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS payment_ref VARCHAR(120),
    ADD COLUMN IF NOT EXISTS gateway VARCHAR(20),
    ADD COLUMN IF NOT EXISTS refund_status VARCHAR(25) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS refund_amount NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS refund_ref VARCHAR(120),
    ADD COLUMN IF NOT EXISTS platform_fee NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS driver_payout NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS settled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS settled_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(40),
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_mobility_trip_payment_order
    ON manacommunity.mobility_trip (payment_order_id)
    WHERE payment_order_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_mobility_trip_driver_settled
    ON manacommunity.mobility_trip (driver_id, settled, status);

-- 4. Carpool / Journey Offer geo coordinates, detour limit, cross-community, slot linkage, school safeguarding
ALTER TABLE manacommunity.mobility_carpool_offer
    ADD COLUMN IF NOT EXISTS origin_lat DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS origin_lng DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS dest_lat DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS dest_lng DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS max_detour_km DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    ADD COLUMN IF NOT EXISTS allow_cross_community BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS source_slot_id BIGINT REFERENCES manacommunity.mobility_journey_slot(id),
    ADD COLUMN IF NOT EXISTS scheduled_date DATE,
    ADD COLUMN IF NOT EXISTS parent_consent_required BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_mob_carpool_slot_date
    ON manacommunity.mobility_carpool_offer (source_slot_id, scheduled_date)
    WHERE source_slot_id IS NOT NULL AND scheduled_date IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_mob_carpool_cross_community
    ON manacommunity.mobility_carpool_offer (allow_cross_community, journey_type, hub_code, status, departure_time);

-- 5. Journey Request geo coordinates, detour limit, cross-community, auto-reserve & school guardian consent
ALTER TABLE manacommunity.mobility_airport_ride_request
    ADD COLUMN IF NOT EXISTS pickup_lat DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS pickup_lng DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS dest_lat DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS dest_lng DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS max_detour_km DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    ADD COLUMN IF NOT EXISTS allow_cross_community BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS auto_reserve BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS parent_consent_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS guardian_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS guardian_phone VARCHAR(25),
    ADD COLUMN IF NOT EXISTS guardian_consent_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN IF NOT EXISTS guardian_consented_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS guardian_approved_by BIGINT;

-- 6. Carpool Booking payment & guardian consent state
ALTER TABLE manacommunity.mobility_carpool_booking
    ADD COLUMN IF NOT EXISTS total_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(25) NOT NULL DEFAULT 'CASH',
    ADD COLUMN IF NOT EXISTS payment_order_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS payment_ref VARCHAR(120),
    ADD COLUMN IF NOT EXISTS refund_amount NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS guardian_consent_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN IF NOT EXISTS guardian_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS guardian_phone VARCHAR(25),
    ADD COLUMN IF NOT EXISTS guardian_consented_at TIMESTAMPTZ;

-- 7. Recurring Journey Slot automation defaults
ALTER TABLE manacommunity.mobility_journey_slot
    ADD COLUMN IF NOT EXISTS origin_lat DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS origin_lng DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS dest_lat DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS dest_lng DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS max_detour_km DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    ADD COLUMN IF NOT EXISTS allow_cross_community BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS auto_generate BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS last_generated_date DATE;
