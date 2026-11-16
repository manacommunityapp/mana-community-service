-- V267: Multi-City Foundation, Service Zones, Hybrid Ownership & Multi-Document Licensing

-- 1. Multi-City Master Configuration
CREATE TABLE IF NOT EXISTS manacommunity.fresh_city (
    id                      BIGSERIAL PRIMARY KEY,
    code                    VARCHAR(20) NOT NULL UNIQUE,
    name                    VARCHAR(100) NOT NULL,
    state                   VARCHAR(100) NOT NULL,
    country                 VARCHAR(100) NOT NULL DEFAULT 'India',
    timezone                VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    is_active               BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_city_code ON manacommunity.fresh_city(code);

-- 2. Service Zones within Cities
CREATE TABLE IF NOT EXISTS manacommunity.fresh_service_zone (
    id                      BIGSERIAL PRIMARY KEY,
    city_id                 BIGINT NOT NULL REFERENCES manacommunity.fresh_city(id) ON DELETE CASCADE,
    name                    VARCHAR(100) NOT NULL,
    code                    VARCHAR(50) NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT true,
    delivery_cutoff_time    TIME NOT NULL DEFAULT '22:00:00',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_fresh_zone_city_code UNIQUE (city_id, code)
);
CREATE INDEX IF NOT EXISTS idx_fresh_zone_city ON manacommunity.fresh_service_zone(city_id);

-- 3. Service Zone Postal Code Mappings
CREATE TABLE IF NOT EXISTS manacommunity.fresh_service_zone_postal_code (
    id                      BIGSERIAL PRIMARY KEY,
    zone_id                 BIGINT NOT NULL REFERENCES manacommunity.fresh_service_zone(id) ON DELETE CASCADE,
    postal_code             VARCHAR(10) NOT NULL,
    locality                VARCHAR(100),
    CONSTRAINT uk_fresh_zone_postal UNIQUE (zone_id, postal_code)
);
CREATE INDEX IF NOT EXISTS idx_fresh_zone_postal ON manacommunity.fresh_service_zone_postal_code(postal_code);

-- 4. Fulfilment Locations (Vendor Facility, Mana Central Hub, Pickup Points)
CREATE TABLE IF NOT EXISTS manacommunity.fresh_fulfilment_location (
    id                      BIGSERIAL PRIMARY KEY,
    city_id                 BIGINT NOT NULL REFERENCES manacommunity.fresh_city(id) ON DELETE CASCADE,
    zone_id                 BIGINT REFERENCES manacommunity.fresh_service_zone(id) ON DELETE SET NULL,
    name                    VARCHAR(120) NOT NULL,
    location_type           VARCHAR(50) NOT NULL DEFAULT 'MANA_HUB', -- VENDOR_FACILITY, MANA_HUB, COLLECTION_POINT, PICKUP_POINT
    address_line1           VARCHAR(255),
    postal_code             VARCHAR(10),
    contact_person          VARCHAR(100),
    contact_phone           VARCHAR(20),
    is_active               BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_fulfil_city ON manacommunity.fresh_fulfilment_location(city_id);

-- 5. Extend Community with Service Zone mapping
ALTER TABLE manacommunity.community 
ADD COLUMN IF NOT EXISTS service_zone_id BIGINT REFERENCES manacommunity.fresh_service_zone(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_comm_service_zone ON manacommunity.community(service_zone_id);

-- 6. Extend Product Catalog Profile with Hybrid Ownership & Fulfilment Attributes
ALTER TABLE manacommunity.fresh_product_catalog_profile
ADD COLUMN IF NOT EXISTS business_model VARCHAR(40) NOT NULL DEFAULT 'MARKETPLACE',
ADD COLUMN IF NOT EXISTS inventory_owner_type VARCHAR(40) NOT NULL DEFAULT 'VENDOR',
ADD COLUMN IF NOT EXISTS fulfilment_owner_type VARCHAR(40) NOT NULL DEFAULT 'VENDOR',
ADD COLUMN IF NOT EXISTS delivery_mode VARCHAR(40) NOT NULL DEFAULT 'VENDOR_DELIVERY',
ADD COLUMN IF NOT EXISTS seller_of_record VARCHAR(120) DEFAULT 'VENDOR';

-- 7. Multi-Document Vendor Compliance Records
CREATE TABLE IF NOT EXISTS manacommunity.fresh_vendor_document (
    id                      BIGSERIAL PRIMARY KEY,
    vms_vendor_id           BIGINT NOT NULL REFERENCES manacommunity.vms_vendors(id) ON DELETE CASCADE,
    document_type           VARCHAR(60) NOT NULL, -- FSSAI_LICENSE, GST_CERTIFICATE, TRADE_LICENSE, POLLUTION_NOC, WATER_TEST_REPORT, FSMS_CERTIFICATE
    document_number         VARCHAR(100),
    valid_from              DATE,
    valid_to                DATE,
    file_key                VARCHAR(500),
    file_url                VARCHAR(500),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION', -- PENDING_VERIFICATION, VERIFIED, REJECTED, EXPIRED
    verified_by_user_id     BIGINT REFERENCES manacommunity.app_user(id) ON DELETE SET NULL,
    verified_at             TIMESTAMPTZ,
    rejection_reason        VARCHAR(500),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fresh_vendor_doc_vendor ON manacommunity.fresh_vendor_document(vms_vendor_id);
CREATE INDEX IF NOT EXISTS idx_fresh_vendor_doc_type ON manacommunity.fresh_vendor_document(vms_vendor_id, document_type);

-- 8. Seed Baseline Multi-City & Service Zone Data
INSERT INTO manacommunity.fresh_city (code, name, state, country, timezone, is_active)
VALUES
    ('HYD', 'Hyderabad', 'Telangana', 'India', 'Asia/Kolkata', true),
    ('BLR', 'Bengaluru', 'Karnataka', 'India', 'Asia/Kolkata', true),
    ('PUN', 'Pune', 'Maharashtra', 'India', 'Asia/Kolkata', true)
ON CONFLICT (code) DO NOTHING;

-- Insert Hyderabad Zones
INSERT INTO manacommunity.fresh_service_zone (city_id, name, code, delivery_cutoff_time, is_active)
SELECT id, 'Nizampet', 'HYD-NZP', '22:00:00'::TIME, true FROM manacommunity.fresh_city WHERE code = 'HYD'
ON CONFLICT (city_id, code) DO NOTHING;

INSERT INTO manacommunity.fresh_service_zone (city_id, name, code, delivery_cutoff_time, is_active)
SELECT id, 'Miyapur', 'HYD-MYP', '22:00:00'::TIME, true FROM manacommunity.fresh_city WHERE code = 'HYD'
ON CONFLICT (city_id, code) DO NOTHING;

INSERT INTO manacommunity.fresh_service_zone (city_id, name, code, delivery_cutoff_time, is_active)
SELECT id, 'Gachibowli', 'HYD-GCB', '22:00:00'::TIME, true FROM manacommunity.fresh_city WHERE code = 'HYD'
ON CONFLICT (city_id, code) DO NOTHING;

-- Insert Bengaluru Zones
INSERT INTO manacommunity.fresh_service_zone (city_id, name, code, delivery_cutoff_time, is_active)
SELECT id, 'Whitefield', 'BLR-WTF', '22:00:00'::TIME, true FROM manacommunity.fresh_city WHERE code = 'BLR'
ON CONFLICT (city_id, code) DO NOTHING;

INSERT INTO manacommunity.fresh_service_zone (city_id, name, code, delivery_cutoff_time, is_active)
SELECT id, 'Sarjapur', 'BLR-SJP', '22:00:00'::TIME, true FROM manacommunity.fresh_city WHERE code = 'BLR'
ON CONFLICT (city_id, code) DO NOTHING;

INSERT INTO manacommunity.fresh_service_zone (city_id, name, code, delivery_cutoff_time, is_active)
SELECT id, 'Bellary Road', 'BLR-BLR', '22:00:00'::TIME, true FROM manacommunity.fresh_city WHERE code = 'BLR'
ON CONFLICT (city_id, code) DO NOTHING;

-- Insert Pune Zones
INSERT INTO manacommunity.fresh_service_zone (city_id, name, code, delivery_cutoff_time, is_active)
SELECT id, 'Wakad', 'PUN-WKD', '22:00:00'::TIME, true FROM manacommunity.fresh_city WHERE code = 'PUN'
ON CONFLICT (city_id, code) DO NOTHING;

INSERT INTO manacommunity.fresh_service_zone (city_id, name, code, delivery_cutoff_time, is_active)
SELECT id, 'Hinjawadi', 'PUN-HNJ', '22:00:00'::TIME, true FROM manacommunity.fresh_city WHERE code = 'PUN'
ON CONFLICT (city_id, code) DO NOTHING;
