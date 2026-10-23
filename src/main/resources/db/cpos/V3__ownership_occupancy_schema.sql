-- ============================================================
-- V3: Ownership & Occupancy Schema
-- MODULE 2: Property Ownership Engine
-- MODULE 3: Occupancy & Resident Lifecycle
-- ============================================================

-- ---- OWNERSHIP ENGINE ----

-- Property Owners (all current owners)
CREATE TABLE property_owners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id),
    user_id UUID REFERENCES users(id),

    owner_type VARCHAR(50) NOT NULL, -- INDIVIDUAL, JOINT, NRI, TRUST, COMPANY, INVESTOR
    ownership_share_pct DECIMAL(5,2) DEFAULT 100.0,
    is_primary_owner BOOLEAN DEFAULT true,

    -- Personal Info (for non-platform users)
    full_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(20),
    aadhaar VARCHAR(20),
    pan VARCHAR(20),
    passport_number VARCHAR(50),
    country_of_residence VARCHAR(100),

    -- Acquisition Details
    purchase_date DATE,
    purchase_price DECIMAL(15,2),
    registration_date DATE,
    registration_number VARCHAR(100),
    stamp_duty DECIMAL(12,2),
    registration_charges DECIMAL(12,2),

    -- Ownership Status
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, TRANSFERRED, INHERITED, GIFTED
    effective_from DATE,
    effective_to DATE,

    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Ownership History (complete chain of title)
CREATE TABLE ownership_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id),
    from_owner_name VARCHAR(255),
    to_owner_name VARCHAR(255),
    transfer_type VARCHAR(50), -- SALE, GIFT_DEED, INHERITANCE, PARTITION, COURT_ORDER
    transfer_date DATE,
    transfer_amount DECIMAL(15,2),
    deed_number VARCHAR(100),
    deed_date DATE,
    notes TEXT,
    document_url TEXT,
    recorded_by UUID,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Nominees
CREATE TABLE nominee_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id),
    owner_id UUID NOT NULL REFERENCES property_owners(id),
    nominee_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(100),
    nominee_share_pct DECIMAL(5,2) DEFAULT 100.0,
    dob DATE,
    aadhaar VARCHAR(20),
    is_minor BOOLEAN DEFAULT false,
    guardian_name VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ---- OCCUPANCY ENGINE ----

-- Occupancy Records
CREATE TABLE occupancy_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id),
    occupancy_type VARCHAR(50) NOT NULL, -- OWNER, TENANT, GUEST, VACANT
    start_date DATE NOT NULL,
    end_date DATE,
    is_current BOOLEAN DEFAULT true,
    move_in_completed BOOLEAN DEFAULT false,
    move_out_completed BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Residents (individual people linked to a property)
CREATE TABLE residents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    community_id UUID NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id),
    user_id UUID REFERENCES users(id),
    occupancy_record_id UUID REFERENCES occupancy_records(id),

    resident_type VARCHAR(50), -- OWNER, TENANT, FAMILY, DOMESTIC_STAFF, CARETAKER
    full_name VARCHAR(255) NOT NULL,
    relation_to_primary VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(20),
    aadhaar VARCHAR(20),

    -- Digital Identity
    rfid_card_number VARCHAR(100),
    qr_code_token VARCHAR(255),
    access_card_number VARCHAR(100),
    is_primary_resident BOOLEAN DEFAULT false,

    move_in_date DATE,
    move_out_date DATE,
    is_active BOOLEAN DEFAULT true,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Vehicles
CREATE TABLE resident_vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    resident_id UUID NOT NULL REFERENCES residents(id),
    vehicle_type VARCHAR(50), -- CAR, TWO_WHEELER, EV_CAR, EV_BIKE, TRUCK, AUTO
    vehicle_number VARCHAR(50) NOT NULL,
    vehicle_brand VARCHAR(100),
    vehicle_model VARCHAR(100),
    vehicle_color VARCHAR(50),
    is_ev BOOLEAN DEFAULT false,
    rfid_tag VARCHAR(100),
    parking_slot_id UUID,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Parking Slots
CREATE TABLE parking_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    community_id UUID NOT NULL,
    tower_id UUID,
    slot_number VARCHAR(50) NOT NULL,
    slot_type VARCHAR(50), -- COVERED, OPEN, EV_CHARGING, HANDICAPPED
    is_assigned BOOLEAN DEFAULT false,
    assigned_property_id UUID,
    assigned_resident_id UUID,
    has_ev_charger BOOLEAN DEFAULT false,
    monthly_charge DECIMAL(8,2),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Domestic Staff
CREATE TABLE domestic_staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id),
    staff_type VARCHAR(50), -- MAID, COOK, DRIVER, SECURITY, GARDENER, NANNY
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    aadhaar VARCHAR(20),
    police_verification_done BOOLEAN DEFAULT false,
    police_verification_date DATE,
    rfid_card_number VARCHAR(100),
    work_days VARCHAR(100), -- MON,TUE,WED
    work_hours VARCHAR(50), -- 09:00-11:00
    agency_name VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes
CREATE INDEX idx_owners_property ON property_owners(property_id);
CREATE INDEX idx_occupancy_property ON occupancy_records(property_id);
CREATE INDEX idx_residents_property ON residents(property_id);
CREATE INDEX idx_residents_community ON residents(community_id);
CREATE INDEX idx_vehicles_resident ON resident_vehicles(resident_id);
CREATE INDEX idx_parking_community ON parking_slots(community_id);
