-- V263: Mana Fresh Vendor Capabilities, Scoped Staff Roles, and Service Areas
-- Supports capability-based operational permissions (Fruit, Fresh-Cut, Batter, Dairy, Nutrition, Meat, Seafood, Delivery)
-- and fine-grained vendor employee roles.

-- 1. Fresh Vendor Capabilities
CREATE TABLE IF NOT EXISTS manacommunity.fresh_vendor_capability (
    id                     BIGSERIAL PRIMARY KEY,
    vms_vendor_id          BIGINT NOT NULL REFERENCES manacommunity.vms_vendors(id) ON DELETE CASCADE,
    capability_type        VARCHAR(50) NOT NULL,
    status                 VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
    approved_by_user_id    BIGINT REFERENCES manacommunity.app_user(id),
    approved_at            TIMESTAMP,
    rejection_reason       VARCHAR(500),
    fssai_license_number   VARCHAR(100),
    fssai_valid_until      DATE,
    facility_address       VARCHAR(500),
    cold_storage_available BOOLEAN NOT NULL DEFAULT FALSE,
    daily_capacity_units   INT NOT NULL DEFAULT 0,
    operating_days         VARCHAR(50) NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI,SAT,SUN',
    created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_fresh_vendor_capability UNIQUE (vms_vendor_id, capability_type)
);

CREATE INDEX IF NOT EXISTS idx_fresh_vendor_cap_vendor ON manacommunity.fresh_vendor_capability(vms_vendor_id);
CREATE INDEX IF NOT EXISTS idx_fresh_vendor_cap_type_status ON manacommunity.fresh_vendor_capability(capability_type, status);

-- 2. Fresh Vendor Staff and Scoped Roles
CREATE TABLE IF NOT EXISTS manacommunity.fresh_vendor_staff (
    id            BIGSERIAL PRIMARY KEY,
    vms_vendor_id BIGINT NOT NULL REFERENCES manacommunity.vms_vendors(id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    vendor_role   VARCHAR(40) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    invited_by    BIGINT REFERENCES manacommunity.app_user(id),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_fresh_vendor_staff UNIQUE (vms_vendor_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_fresh_vendor_staff_vendor ON manacommunity.fresh_vendor_staff(vms_vendor_id);
CREATE INDEX IF NOT EXISTS idx_fresh_vendor_staff_user ON manacommunity.fresh_vendor_staff(user_id);

-- 3. Fresh Vendor Serviceable Communities & Cut-off Times
CREATE TABLE IF NOT EXISTS manacommunity.fresh_vendor_service_area (
    id                    BIGSERIAL PRIMARY KEY,
    vms_vendor_id         BIGINT NOT NULL REFERENCES manacommunity.vms_vendors(id) ON DELETE CASCADE,
    community_id          BIGINT NOT NULL REFERENCES manacommunity.community(id),
    cutoff_time           TIME NOT NULL DEFAULT '22:00:00',
    morning_delivery_slot VARCHAR(50) NOT NULL DEFAULT '06:00_08:00',
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_fresh_vendor_community UNIQUE (vms_vendor_id, community_id)
);

CREATE INDEX IF NOT EXISTS idx_fresh_vendor_sa_comm ON manacommunity.fresh_vendor_service_area(community_id);
CREATE INDEX IF NOT EXISTS idx_fresh_vendor_sa_vendor ON manacommunity.fresh_vendor_service_area(vms_vendor_id);
