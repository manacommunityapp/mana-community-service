-- ============================================================
-- V1: Core Tenant & Community Schema
-- CPOS — Community Property Operating System
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Tenants (SaaS Clients — each community / builder / management company)
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_code VARCHAR(50) UNIQUE NOT NULL,
    tenant_name VARCHAR(255) NOT NULL,
    tenant_type VARCHAR(50) NOT NULL, -- COMMUNITY, BUILDER, MANAGEMENT_COMPANY, ENTERPRISE
    subscription_plan VARCHAR(50) DEFAULT 'FREE', -- FREE, PRO, ENTERPRISE
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    pincode VARCHAR(20),
    logo_url TEXT,
    is_active BOOLEAN DEFAULT true,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Communities (Each gated community / society / township)
CREATE TABLE communities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    community_code VARCHAR(50) UNIQUE NOT NULL,
    community_name VARCHAR(255) NOT NULL,
    community_type VARCHAR(50), -- GATED_SOCIETY, TOWNSHIP, STANDALONE, COMMERCIAL_COMPLEX
    total_units INT,
    total_towers INT,
    total_floors INT,
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    google_maps_url TEXT,
    established_year INT,
    rera_number VARCHAR(100),
    rera_expiry_date DATE,
    amenities_json JSONB DEFAULT '[]',
    is_active BOOLEAN DEFAULT true,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Towers / Buildings
CREATE TABLE towers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    community_id UUID NOT NULL REFERENCES communities(id),
    tower_code VARCHAR(50) NOT NULL,
    tower_name VARCHAR(255) NOT NULL,
    total_floors INT,
    total_units INT,
    construction_year INT,
    wing VARCHAR(10),
    has_lift BOOLEAN DEFAULT true,
    has_basement BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Floors
CREATE TABLE floors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    tower_id UUID NOT NULL REFERENCES towers(id),
    floor_number INT NOT NULL,
    floor_label VARCHAR(50), -- Ground, 1st, Mezzanine, Terrace
    total_units INT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Users (platform-level users spanning all roles)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE,
    password_hash TEXT,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    display_name VARCHAR(200),
    avatar_url TEXT,
    date_of_birth DATE,
    gender VARCHAR(20),
    nationality VARCHAR(100),
    aadhaar_number VARCHAR(20),
    pan_number VARCHAR(20),
    is_kyc_verified BOOLEAN DEFAULT false,
    is_nri BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    is_deleted BOOLEAN DEFAULT false,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- User Roles (RBAC)
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_code VARCHAR(50) UNIQUE NOT NULL, -- SUPER_ADMIN, ADMIN, OWNER, TENANT, BUILDER, BROKER, VENDOR
    role_name VARCHAR(100) NOT NULL,
    description TEXT,
    permissions_json JSONB DEFAULT '[]'
);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    tenant_id UUID NOT NULL,
    community_id UUID,
    granted_at TIMESTAMPTZ DEFAULT now(),
    granted_by UUID,
    is_active BOOLEAN DEFAULT true
);

-- Audit Logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID,
    entity_type VARCHAR(100),
    entity_id UUID,
    action VARCHAR(50), -- CREATE, UPDATE, DELETE, VIEW, EXPORT
    old_values_json JSONB,
    new_values_json JSONB,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes
CREATE INDEX idx_communities_tenant ON communities(tenant_id);
CREATE INDEX idx_towers_community ON towers(community_id);
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
