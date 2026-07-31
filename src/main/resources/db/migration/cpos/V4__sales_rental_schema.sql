-- V4__sales_rental_schema.sql
-- Migration for Sales and Rental Schema

CREATE TABLE property_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    community_id UUID,
    property_id UUID NOT NULL,
    listing_type VARCHAR(50) NOT NULL, -- SALE/RENT
    listing_price DECIMAL(15,2) NOT NULL,
    is_negotiable BOOLEAN DEFAULT false,
    listing_status VARCHAR(50) NOT NULL, -- ACTIVE/SOLD/RENTED/EXPIRED
    listed_by UUID,
    highlights_json JSONB DEFAULT '[]',
    available_from DATE,
    views_count INT DEFAULT 0,
    enquiry_count INT DEFAULT 0,
    is_featured BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE buyer_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID,
    listing_id UUID REFERENCES property_listings(id),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    budget_min DECIMAL(15,2),
    budget_max DECIMAL(15,2),
    property_type_preference VARCHAR(100),
    bedrooms INT,
    area_preference VARCHAR(100),
    lead_source VARCHAR(100),
    lead_status VARCHAR(50), -- NEW/CONTACTED/SITE_VISIT_SCHEDULED/NEGOTIATING/CONVERTED/LOST
    assigned_to UUID,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE site_visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID,
    lead_id UUID REFERENCES buyer_leads(id),
    visitor_name VARCHAR(255),
    visitor_phone VARCHAR(50),
    scheduled_date DATE,
    scheduled_time TIME,
    visit_type VARCHAR(50), -- PHYSICAL/VIRTUAL
    status VARCHAR(50), -- SCHEDULED/COMPLETED/CANCELLED/NO_SHOW
    conducted_by UUID,
    feedback_text TEXT,
    rating INT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE property_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    listing_id UUID REFERENCES property_listings(id),
    buyer_lead_id UUID REFERENCES buyer_leads(id),
    offer_amount DECIMAL(15,2),
    offer_type VARCHAR(50), -- SALE/RENT
    offer_date DATE,
    validity_date DATE,
    offer_status VARCHAR(50), -- PENDING/ACCEPTED/REJECTED/COUNTER_OFFERED/WITHDRAWN
    counter_offer_amount DECIMAL(15,2),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE sale_agreements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    buyer_id UUID NOT NULL,
    agreed_price DECIMAL(15,2),
    token_amount DECIMAL(12,2),
    agreement_date DATE,
    registration_target_date DATE,
    status VARCHAR(50), -- DRAFT/SIGNED/REGISTERED/CANCELLED
    document_url TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE rental_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    monthly_rent DECIMAL(10,2),
    security_deposit DECIMAL(12,2),
    deposit_months INT DEFAULT 2,
    is_deposit_negotiable BOOLEAN DEFAULT false,
    maintenance_included BOOLEAN DEFAULT false,
    available_from DATE,
    preferred_tenant_type VARCHAR(50), -- FAMILY/BACHELOR/PROFESSIONAL/ANY
    pet_allowed BOOLEAN DEFAULT false,
    listing_status VARCHAR(50), -- ACTIVE/RENTED/EXPIRED
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE tenant_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    rental_listing_id UUID REFERENCES rental_listings(id),
    applicant_name VARCHAR(255),
    applicant_email VARCHAR(255),
    applicant_phone VARCHAR(50),
    profession VARCHAR(100),
    monthly_income DECIMAL(12,2),
    family_size INT,
    has_pets BOOLEAN,
    application_status VARCHAR(50), -- APPLIED/SHORTLISTED/VERIFICATION_PENDING/VERIFIED/REJECTED/APPROVED
    applied_on TIMESTAMPTZ DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE lease_agreements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    tenant_resident_id UUID NOT NULL,
    monthly_rent DECIMAL(10,2),
    security_deposit DECIMAL(12,2),
    lease_start DATE,
    lease_end DATE,
    notice_period_days INT DEFAULT 30,
    rent_escalation_pct DECIMAL(4,2),
    escalation_frequency_months INT DEFAULT 12,
    lease_status VARCHAR(50), -- DRAFT/ACTIVE/EXPIRED/TERMINATED
    document_url TEXT,
    esign_status VARCHAR(50), -- PENDING/SIGNED
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE rent_collections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    lease_id UUID REFERENCES lease_agreements(id),
    due_date DATE,
    amount_due DECIMAL(10,2),
    amount_paid DECIMAL(10,2),
    payment_date DATE,
    payment_mode VARCHAR(50),
    transaction_ref VARCHAR(100),
    payment_status VARCHAR(50), -- PENDING/PAID/PARTIAL/OVERDUE
    late_fee DECIMAL(8,2),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes
CREATE INDEX idx_property_listings_prop_id ON property_listings(property_id);
CREATE INDEX idx_property_listings_tenant_id ON property_listings(tenant_id);
CREATE INDEX idx_property_listings_status ON property_listings(listing_status);

CREATE INDEX idx_buyer_leads_prop_id ON buyer_leads(property_id);
CREATE INDEX idx_buyer_leads_tenant_id ON buyer_leads(tenant_id);
CREATE INDEX idx_buyer_leads_status ON buyer_leads(lead_status);

CREATE INDEX idx_site_visits_prop_id ON site_visits(property_id);
CREATE INDEX idx_site_visits_tenant_id ON site_visits(tenant_id);
CREATE INDEX idx_site_visits_status ON site_visits(status);

CREATE INDEX idx_property_offers_prop_id ON property_offers(property_id);
CREATE INDEX idx_property_offers_tenant_id ON property_offers(tenant_id);
CREATE INDEX idx_property_offers_status ON property_offers(offer_status);

CREATE INDEX idx_sale_agreements_prop_id ON sale_agreements(property_id);
CREATE INDEX idx_sale_agreements_tenant_id ON sale_agreements(tenant_id);
CREATE INDEX idx_sale_agreements_status ON sale_agreements(status);

CREATE INDEX idx_rental_listings_prop_id ON rental_listings(property_id);
CREATE INDEX idx_rental_listings_tenant_id ON rental_listings(tenant_id);
CREATE INDEX idx_rental_listings_status ON rental_listings(listing_status);

CREATE INDEX idx_tenant_applications_prop_id ON tenant_applications(property_id);
CREATE INDEX idx_tenant_applications_tenant_id ON tenant_applications(tenant_id);
CREATE INDEX idx_tenant_applications_status ON tenant_applications(application_status);

CREATE INDEX idx_lease_agreements_prop_id ON lease_agreements(property_id);
CREATE INDEX idx_lease_agreements_tenant_id ON lease_agreements(tenant_id);
CREATE INDEX idx_lease_agreements_status ON lease_agreements(lease_status);

CREATE INDEX idx_rent_collections_prop_id ON rent_collections(property_id);
CREATE INDEX idx_rent_collections_tenant_id ON rent_collections(tenant_id);
CREATE INDEX idx_rent_collections_status ON rent_collections(payment_status);
