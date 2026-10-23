-- ============================================================
-- V2: Property Master Schema — Digital Twin Core
-- CPOS — Community Property Operating System
-- MODULE 1: Property Master Engine
-- ============================================================

-- Property Types Enum Reference
CREATE TABLE property_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_code VARCHAR(50) UNIQUE NOT NULL,
    type_name VARCHAR(100) NOT NULL,
    category VARCHAR(50), -- RESIDENTIAL, COMMERCIAL, LAND, PARKING
    is_active BOOLEAN DEFAULT true
);

INSERT INTO property_types (type_code, type_name, category) VALUES
('APARTMENT', 'Apartment', 'RESIDENTIAL'),
('VILLA', 'Villa', 'RESIDENTIAL'),
('INDEPENDENT_HOUSE', 'Independent House', 'RESIDENTIAL'),
('DUPLEX', 'Duplex', 'RESIDENTIAL'),
('STUDIO', 'Studio Apartment', 'RESIDENTIAL'),
('PENTHOUSE', 'Penthouse', 'RESIDENTIAL'),
('ROW_HOUSE', 'Row House', 'RESIDENTIAL'),
('COMMERCIAL_OFFICE', 'Commercial Office', 'COMMERCIAL'),
('RETAIL_SHOP', 'Retail Shop', 'COMMERCIAL'),
('WAREHOUSE', 'Warehouse', 'COMMERCIAL'),
('GUEST_HOUSE', 'Guest House', 'COMMERCIAL'),
('PARKING_COVERED', 'Covered Parking', 'PARKING'),
('PARKING_OPEN', 'Open Parking', 'PARKING'),
('STORAGE_UNIT', 'Storage Unit', 'PARKING'),
('LAND', 'Land / Plot', 'LAND'),
('BUILDING', 'Entire Building', 'COMMERCIAL');

-- Core Property Entity — The Digital Twin
CREATE TABLE properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    community_id UUID NOT NULL REFERENCES communities(id),
    tower_id UUID REFERENCES towers(id),
    floor_id UUID REFERENCES floors(id),

    -- Digital Identity
    property_code VARCHAR(100) UNIQUE NOT NULL, -- CPOS-{communityCode}-{propertyCode}
    property_name VARCHAR(255),
    property_type_code VARCHAR(50) NOT NULL REFERENCES property_types(type_code),

    -- Location
    unit_number VARCHAR(50) NOT NULL,
    floor_number INT,
    wing VARCHAR(20),
    facing VARCHAR(50), -- NORTH, SOUTH, EAST, WEST, NORTH_EAST, etc.
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),

    -- Area Measurements (sq ft)
    carpet_area DECIMAL(10,2),
    built_up_area DECIMAL(10,2),
    super_built_up_area DECIMAL(10,2),
    plot_area DECIMAL(10,2),

    -- Configuration
    bedrooms INT DEFAULT 0,
    bathrooms INT DEFAULT 0,
    balconies INT DEFAULT 0,
    floor_count INT DEFAULT 1, -- for duplex/triplex
    total_floors_in_building INT,
    furnished_status VARCHAR(50), -- UNFURNISHED, SEMI_FURNISHED, FULLY_FURNISHED

    -- Parking
    covered_parking INT DEFAULT 0,
    open_parking INT DEFAULT 0,
    parking_slot_numbers VARCHAR(255),

    -- Status
    construction_status VARCHAR(50) DEFAULT 'READY', -- UNDER_CONSTRUCTION, READY, POSSESSION_GIVEN
    occupancy_status VARCHAR(50) DEFAULT 'VACANT', -- OWNER_OCCUPIED, TENANT_OCCUPIED, VACANT, RESERVED, BUILDER_INVENTORY
    property_status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, LISTED_FOR_SALE, LISTED_FOR_RENT, UNDER_RENOVATION, DEMOLISHED

    -- Registration
    khata_number VARCHAR(100),
    pid_number VARCHAR(100),
    survey_number VARCHAR(100),
    registration_number VARCHAR(100),
    rera_unit_number VARCHAR(100),

    -- Construction Details
    construction_year INT,
    possession_date DATE,
    age_years INT,

    -- Amenities & Features
    amenities_json JSONB DEFAULT '[]', -- e.g. ["swimming_pool", "gym", "club_house"]
    features_json JSONB DEFAULT '{}', -- e.g. {"has_solar": true, "has_ev_charging": false}

    -- Media
    primary_image_url TEXT,

    -- Financial (quick reference)
    last_purchase_price DECIMAL(15,2),
    current_market_value DECIMAL(15,2),
    monthly_maintenance DECIMAL(10,2),

    -- Soft delete + audit
    is_deleted BOOLEAN DEFAULT false,
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    version BIGINT DEFAULT 0
);

-- Property Images
CREATE TABLE property_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id),
    image_url TEXT NOT NULL,
    image_type VARCHAR(50), -- EXTERIOR, INTERIOR, FLOOR_PLAN, BLUEPRINT, 360_VIEW
    caption VARCHAR(255),
    sort_order INT DEFAULT 0,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Property Videos
CREATE TABLE property_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id),
    video_url TEXT NOT NULL,
    video_type VARCHAR(50), -- WALKTHROUGH, DRONE, VIRTUAL_TOUR, 3D_TOUR
    title VARCHAR(255),
    duration_seconds INT,
    thumbnail_url TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Floor Plans
CREATE TABLE property_floor_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id),
    plan_name VARCHAR(100),
    plan_url TEXT NOT NULL,
    plan_type VARCHAR(50), -- 2D, 3D, CAD
    version INT DEFAULT 1,
    is_current BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Property Attributes (EAV for extensible custom attributes)
CREATE TABLE property_attributes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties(id),
    attribute_key VARCHAR(100) NOT NULL,
    attribute_value TEXT,
    attribute_type VARCHAR(50) -- TEXT, NUMBER, BOOLEAN, DATE
);

-- Property History (append-only lifecycle log)
CREATE TABLE property_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL REFERENCES properties(id),
    event_type VARCHAR(100) NOT NULL, -- CREATED, OWNERSHIP_TRANSFERRED, TENANTED, VACATED, RENOVATED, LISTED, SOLD, etc.
    event_date DATE NOT NULL,
    event_description TEXT,
    event_data_json JSONB,
    recorded_by UUID,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes
CREATE INDEX idx_properties_tenant ON properties(tenant_id);
CREATE INDEX idx_properties_community ON properties(community_id);
CREATE INDEX idx_properties_tower ON properties(tower_id);
CREATE INDEX idx_properties_code ON properties(property_code);
CREATE INDEX idx_properties_status ON properties(occupancy_status, property_status);
CREATE INDEX idx_properties_type ON properties(property_type_code);
CREATE INDEX idx_property_history_property ON property_history(property_id);
CREATE INDEX idx_property_history_event ON property_history(event_type);
