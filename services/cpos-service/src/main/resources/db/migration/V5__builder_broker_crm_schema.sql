-- V5__builder_broker_crm_schema.sql
-- Migration for Builder, Broker, and CRM Schema

CREATE TABLE builders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    builder_code VARCHAR(100) UNIQUE NOT NULL,
    builder_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    rera_registration VARCHAR(100),
    address TEXT,
    city VARCHAR(100),
    total_projects INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE builder_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    builder_id UUID REFERENCES builders(id),
    project_code VARCHAR(100) UNIQUE NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    project_type VARCHAR(50), -- RESIDENTIAL/COMMERCIAL/MIXED
    rera_number VARCHAR(100),
    total_towers INT,
    total_units INT,
    total_sold INT DEFAULT 0,
    construction_status VARCHAR(50), -- PLANNING/UNDER_CONSTRUCTION/NEAR_COMPLETION/COMPLETED
    possession_date DATE,
    project_description TEXT,
    address TEXT,
    city VARCHAR(100),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    price_range_min DECIMAL(12,2),
    price_range_max DECIMAL(12,2),
    amenities_json JSONB DEFAULT '[]',
    gallery_urls_json JSONB DEFAULT '[]',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE construction_phases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES builder_projects(id),
    phase_name VARCHAR(255),
    phase_number INT,
    start_date DATE,
    expected_end_date DATE,
    actual_end_date DATE,
    completion_pct DECIMAL(5,2) DEFAULT 0.0,
    status VARCHAR(50), -- PLANNED/IN_PROGRESS/COMPLETED/DELAYED
    update_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE builder_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    project_id UUID REFERENCES builder_projects(id),
    property_id UUID,
    buyer_name VARCHAR(255),
    buyer_email VARCHAR(255),
    buyer_phone VARCHAR(50),
    booking_amount DECIMAL(12,2),
    total_cost DECIMAL(15,2),
    booking_date DATE,
    payment_plan VARCHAR(100),
    booking_status VARCHAR(50), -- BOOKED/AGREEMENT_SIGNED/REGISTERED/CANCELLED
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE brokers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    broker_code VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    rera_number VARCHAR(100),
    specialization VARCHAR(100),
    service_areas_json JSONB DEFAULT '[]',
    commission_rate_sale DECIMAL(4,2) DEFAULT 2.0,
    commission_rate_rent DECIMAL(4,2) DEFAULT 8.33,
    rating DECIMAL(3,1) DEFAULT 0.0,
    total_deals INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE broker_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    broker_id UUID REFERENCES brokers(id),
    property_id UUID,
    lead_name VARCHAR(255),
    lead_phone VARCHAR(50),
    lead_type VARCHAR(50), -- SALE/RENT
    status VARCHAR(50), -- ASSIGNED/WORKING/CONVERTED/LOST
    assigned_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE broker_commissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    broker_id UUID REFERENCES brokers(id),
    property_id UUID,
    deal_type VARCHAR(50), -- SALE/RENT
    deal_value DECIMAL(15,2),
    commission_pct DECIMAL(4,2),
    commission_amount DECIMAL(12,2),
    status VARCHAR(50), -- PENDING/APPROVED/PAID
    payment_date DATE,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE crm_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    source VARCHAR(100),
    lead_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    property_type_interest VARCHAR(100),
    budget_range VARCHAR(100),
    locality_preference VARCHAR(255),
    lead_type VARCHAR(50), -- SALE/RENT/INVESTMENT
    stage VARCHAR(50), -- NEW/CONTACTED/QUALIFIED/SITE_VISIT/NEGOTIATION/WON/LOST
    assigned_to UUID,
    priority VARCHAR(50), -- HIGH/MEDIUM/LOW
    score INT DEFAULT 0,
    last_activity_at TIMESTAMPTZ,
    expected_close_date DATE,
    notes TEXT,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE crm_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID REFERENCES crm_leads(id),
    activity_type VARCHAR(50), -- CALL/EMAIL/WHATSAPP/SITE_VISIT/MEETING/NOTE
    subject VARCHAR(255),
    description TEXT,
    scheduled_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    outcome VARCHAR(255),
    conducted_by UUID,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes
CREATE INDEX idx_builders_tenant_id ON builders(tenant_id);
CREATE INDEX idx_builder_projects_tenant_id ON builder_projects(tenant_id);
CREATE INDEX idx_builder_projects_builder_id ON builder_projects(builder_id);
CREATE INDEX idx_builder_bookings_tenant_id ON builder_bookings(tenant_id);
CREATE INDEX idx_builder_bookings_project_id ON builder_bookings(project_id);

CREATE INDEX idx_brokers_tenant_id ON brokers(tenant_id);
CREATE INDEX idx_broker_leads_tenant_id ON broker_leads(tenant_id);
CREATE INDEX idx_broker_leads_broker_id ON broker_leads(broker_id);
CREATE INDEX idx_broker_leads_property_id ON broker_leads(property_id);
CREATE INDEX idx_broker_leads_status ON broker_leads(status);

CREATE INDEX idx_broker_commissions_tenant_id ON broker_commissions(tenant_id);
CREATE INDEX idx_broker_commissions_broker_id ON broker_commissions(broker_id);

CREATE INDEX idx_crm_leads_tenant_id ON crm_leads(tenant_id);
CREATE INDEX idx_crm_leads_stage ON crm_leads(stage);

CREATE INDEX idx_crm_activities_tenant_id ON crm_activities(tenant_id);
CREATE INDEX idx_crm_activities_lead_id ON crm_activities(lead_id);
