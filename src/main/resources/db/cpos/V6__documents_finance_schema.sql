-- V6__documents_finance_schema.sql
-- Migration for Documents and Finance Schema

CREATE TABLE document_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_code VARCHAR(100) UNIQUE NOT NULL,
    category_name VARCHAR(255) NOT NULL,
    description TEXT,
    is_required BOOLEAN DEFAULT false,
    applicable_to VARCHAR(100)
);

-- Insert reference data
INSERT INTO document_categories (category_code, category_name) VALUES 
    ('SALE_DEED', 'Sale Deed'),
    ('LEASE_AGREEMENT', 'Lease Agreement'),
    ('KHATA', 'Khata'),
    ('MUTATION', 'Mutation Extract'),
    ('OC_CERTIFICATE', 'Occupancy Certificate (OC)'),
    ('CC_CERTIFICATE', 'Completion Certificate (CC)'),
    ('PROPERTY_TAX', 'Property Tax Receipt'),
    ('ELECTRICITY_BILL', 'Electricity Bill'),
    ('WATER_BILL', 'Water Bill'),
    ('GAS_CONNECTION', 'Gas Connection'),
    ('INSURANCE', 'Property Insurance'),
    ('LOAN_SANCTION', 'Loan Sanction Letter'),
    ('BUILDER_AGREEMENT', 'Builder Agreement'),
    ('NOC_SOCIETY', 'Society NOC'),
    ('ENCUMBRANCE_CERTIFICATE', 'Encumbrance Certificate (EC)'),
    ('FLOOR_PLAN', 'Floor Plan'),
    ('BLUEPRINT', 'Blueprint'),
    ('WILL', 'Will'),
    ('GIFT_DEED', 'Gift Deed'),
    ('INHERITANCE_DEED', 'Inheritance Deed');

CREATE TABLE property_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    owner_id UUID,
    category_code VARCHAR(100) REFERENCES document_categories(category_code),
    document_title VARCHAR(255),
    document_number VARCHAR(100),
    issued_by VARCHAR(255),
    issue_date DATE,
    expiry_date DATE,
    file_url TEXT,
    file_size_kb INT,
    mime_type VARCHAR(100),
    is_verified BOOLEAN DEFAULT false,
    verified_by UUID,
    verified_at TIMESTAMPTZ,
    digital_signature_status VARCHAR(50), -- PENDING/SIGNED/REJECTED
    version INT DEFAULT 1,
    is_current BOOLEAN DEFAULT true,
    notes TEXT,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE document_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    document_id UUID REFERENCES property_documents(id),
    version_number INT,
    file_url TEXT,
    change_description TEXT,
    uploaded_by UUID,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE expiry_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID,
    document_id UUID REFERENCES property_documents(id),
    alert_type VARCHAR(50), -- EXPIRY/RENEWAL
    alert_date DATE,
    days_before_expiry INT,
    is_sent BOOLEAN DEFAULT false,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE property_valuations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    valuation_type VARCHAR(50), -- MARKET/BANK/GOVERNMENT/AI_PREDICTED
    valuation_amount DECIMAL(15,2),
    valuation_date DATE,
    valuation_by VARCHAR(100),
    valuation_method VARCHAR(50), -- COMPARABLE/INCOME/COST/AI
    certificate_url TEXT,
    valid_until DATE,
    is_ai_generated BOOLEAN DEFAULT false,
    ai_confidence_score DECIMAL(5,2),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE property_loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    owner_id UUID,
    bank_name VARCHAR(255),
    loan_account_number VARCHAR(100) UNIQUE,
    loan_type VARCHAR(50), -- HOME/LAP/CONSTRUCTION/COMMERCIAL
    sanction_amount DECIMAL(15,2),
    outstanding_amount DECIMAL(15,2),
    interest_rate DECIMAL(5,2),
    tenure_months INT,
    emi_amount DECIMAL(10,2),
    disbursement_date DATE,
    loan_end_date DATE,
    status VARCHAR(50), -- ACTIVE/CLOSED/DEFAULTED
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE property_tax_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    tax_year VARCHAR(10),
    assessment_value DECIMAL(15,2),
    tax_amount DECIMAL(10,2),
    penalty DECIMAL(8,2),
    total_due DECIMAL(10,2),
    due_date DATE,
    payment_date DATE,
    payment_reference VARCHAR(100),
    is_paid BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE property_insurance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    insurance_type VARCHAR(50), -- STRUCTURAL/CONTENT/BOTH
    provider_name VARCHAR(255),
    policy_number VARCHAR(100) UNIQUE,
    coverage_amount DECIMAL(15,2),
    premium_amount DECIMAL(10,2),
    premium_frequency VARCHAR(50), -- MONTHLY/QUARTERLY/ANNUAL
    start_date DATE,
    end_date DATE,
    status VARCHAR(50), -- ACTIVE/EXPIRED/CANCELLED
    document_url TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE financial_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    snapshot_date DATE,
    purchase_price DECIMAL(15,2),
    current_market_value DECIMAL(15,2),
    outstanding_loan DECIMAL(15,2),
    annual_rental_income DECIMAL(12,2),
    annual_maintenance_cost DECIMAL(10,2),
    annual_property_tax DECIMAL(10,2),
    annual_insurance_premium DECIMAL(10,2),
    gross_rental_yield DECIMAL(5,2),
    net_rental_yield DECIMAL(5,2),
    capital_gain DECIMAL(15,2),
    roi_pct DECIMAL(5,2),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes
CREATE INDEX idx_property_documents_prop_id ON property_documents(property_id);
CREATE INDEX idx_property_documents_tenant_id ON property_documents(tenant_id);
CREATE INDEX idx_property_documents_expiry_date ON property_documents(expiry_date);

CREATE INDEX idx_expiry_alerts_prop_id ON expiry_alerts(property_id);
CREATE INDEX idx_expiry_alerts_tenant_id ON expiry_alerts(tenant_id);

CREATE INDEX idx_property_valuations_prop_id ON property_valuations(property_id);
CREATE INDEX idx_property_valuations_tenant_id ON property_valuations(tenant_id);

CREATE INDEX idx_property_loans_prop_id ON property_loans(property_id);
CREATE INDEX idx_property_loans_tenant_id ON property_loans(tenant_id);

CREATE INDEX idx_property_tax_records_prop_id ON property_tax_records(property_id);
CREATE INDEX idx_property_tax_records_tenant_id ON property_tax_records(tenant_id);
CREATE INDEX idx_property_tax_records_is_paid ON property_tax_records(is_paid);

CREATE INDEX idx_property_insurance_prop_id ON property_insurance(property_id);
CREATE INDEX idx_property_insurance_tenant_id ON property_insurance(tenant_id);

CREATE INDEX idx_financial_snapshots_prop_id ON financial_snapshots(property_id);
CREATE INDEX idx_financial_snapshots_tenant_id ON financial_snapshots(tenant_id);
