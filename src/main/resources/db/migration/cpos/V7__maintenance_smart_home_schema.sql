-- V7__maintenance_smart_home_schema.sql
-- Migration for Maintenance and Smart Home Schema

CREATE TABLE maintenance_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    icon_url TEXT,
    sla_hours INT DEFAULT 48
);

-- Insert categories
INSERT INTO maintenance_categories (code, name) VALUES 
    ('PLUMBING', 'Plumbing'),
    ('ELECTRICAL', 'Electrical'),
    ('CIVIL', 'Civil'),
    ('CARPENTRY', 'Carpentry'),
    ('PAINTING', 'Painting'),
    ('AC_SERVICE', 'AC Service'),
    ('LIFT_MAINTENANCE', 'Lift Maintenance'),
    ('PEST_CONTROL', 'Pest Control'),
    ('HOUSEKEEPING', 'Housekeeping'),
    ('SECURITY_SYSTEM', 'Security System'),
    ('INTERNET', 'Internet'),
    ('GAS', 'Gas'),
    ('WATER_SUPPLY', 'Water Supply'),
    ('COMMON_AREA', 'Common Area'),
    ('INTERCOM', 'Intercom');

CREATE TABLE maintenance_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    community_id UUID,
    property_id UUID NOT NULL,
    resident_id UUID,
    category_code VARCHAR(100) REFERENCES maintenance_categories(code),
    title VARCHAR(255),
    description TEXT,
    priority VARCHAR(50), -- CRITICAL/HIGH/MEDIUM/LOW
    status VARCHAR(50), -- OPEN/ASSIGNED/IN_PROGRESS/ON_HOLD/RESOLVED/CLOSED
    request_number VARCHAR(50) UNIQUE,
    images_json JSONB DEFAULT '[]',
    preferred_visit_date DATE,
    preferred_visit_time VARCHAR(20),
    assigned_vendor_id UUID,
    assigned_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    resident_rating INT,
    resident_feedback TEXT,
    resolution_notes TEXT,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE amcs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    vendor_id UUID,
    amc_type VARCHAR(100),
    equipment_description TEXT,
    start_date DATE,
    end_date DATE,
    annual_cost DECIMAL(10,2),
    visit_frequency VARCHAR(50),
    last_service_date DATE,
    next_service_date DATE,
    status VARCHAR(50), -- ACTIVE/EXPIRED/RENEWED
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE warranty_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    item_name VARCHAR(255),
    brand VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    purchase_date DATE,
    warranty_expiry DATE,
    vendor_name VARCHAR(255),
    service_contact VARCHAR(100),
    document_url TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE vendors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    vendor_code VARCHAR(100) UNIQUE NOT NULL,
    business_name VARCHAR(255),
    category_code VARCHAR(100) REFERENCES maintenance_categories(code),
    owner_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    service_areas_json JSONB DEFAULT '[]',
    rating DECIMAL(3,1) DEFAULT 0.0,
    total_jobs INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE smart_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    device_code VARCHAR(100) UNIQUE NOT NULL,
    device_name VARCHAR(255),
    device_type VARCHAR(100), -- SMART_LOCK/CCTV/EV_CHARGER/SMART_METER/AIR_QUALITY/SOLAR_INVERTER/DOORBELL/THERMOSTAT
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    installation_date DATE,
    firmware_version VARCHAR(50),
    status VARCHAR(50), -- ONLINE/OFFLINE/MAINTENANCE/ERROR
    current_reading_json JSONB DEFAULT '{}',
    last_heartbeat_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE device_readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    device_id UUID REFERENCES smart_devices(id),
    metric_type VARCHAR(100), -- POWER_KWH/WATER_LITERS/TEMPERATURE/HUMIDITY/AQI/CO2
    reading_value DECIMAL(10,3),
    unit VARCHAR(20),
    recorded_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE move_in_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    resident_id UUID,
    occupancy_record_id UUID,
    workflow_status VARCHAR(50), -- PENDING/IN_PROGRESS/COMPLETED
    checklist_json JSONB DEFAULT '{}',
    move_in_date DATE,
    inspection_done BOOLEAN DEFAULT false,
    parking_assigned BOOLEAN DEFAULT false,
    rfid_issued BOOLEAN DEFAULT false,
    access_activated BOOLEAN DEFAULT false,
    community_account_created BOOLEAN DEFAULT false,
    security_notified BOOLEAN DEFAULT false,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE move_out_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    resident_id UUID,
    lease_id UUID,
    move_out_date DATE,
    notice_given_date DATE,
    inspection_done BOOLEAN DEFAULT false,
    damages_found BOOLEAN DEFAULT false,
    damage_deduction DECIMAL(10,2) DEFAULT 0,
    deposit_refund_amount DECIMAL(10,2),
    refund_status VARCHAR(50), -- PENDING/PROCESSED
    access_deactivated BOOLEAN DEFAULT false,
    workflow_status VARCHAR(50), -- PENDING/IN_PROGRESS/COMPLETED
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes
CREATE INDEX idx_maintenance_requests_tenant_id ON maintenance_requests(tenant_id);
CREATE INDEX idx_maintenance_requests_property_id ON maintenance_requests(property_id);
CREATE INDEX idx_maintenance_requests_status ON maintenance_requests(status);

CREATE INDEX idx_amcs_tenant_id ON amcs(tenant_id);
CREATE INDEX idx_amcs_property_id ON amcs(property_id);

CREATE INDEX idx_warranty_records_tenant_id ON warranty_records(tenant_id);
CREATE INDEX idx_warranty_records_property_id ON warranty_records(property_id);

CREATE INDEX idx_vendors_tenant_id ON vendors(tenant_id);

CREATE INDEX idx_smart_devices_tenant_id ON smart_devices(tenant_id);
CREATE INDEX idx_smart_devices_property_id ON smart_devices(property_id);

CREATE INDEX idx_device_readings_tenant_id ON device_readings(tenant_id);
CREATE INDEX idx_device_readings_property_id ON device_readings(property_id);
CREATE INDEX idx_device_readings_device_id ON device_readings(device_id);

CREATE INDEX idx_move_in_workflows_tenant_id ON move_in_workflows(tenant_id);
CREATE INDEX idx_move_in_workflows_property_id ON move_in_workflows(property_id);

CREATE INDEX idx_move_out_workflows_tenant_id ON move_out_workflows(tenant_id);
CREATE INDEX idx_move_out_workflows_property_id ON move_out_workflows(property_id);
