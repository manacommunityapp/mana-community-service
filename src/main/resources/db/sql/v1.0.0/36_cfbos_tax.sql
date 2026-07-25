-- src/main/resources/db/sql/v1.0.0/36_cfbos_tax.sql

CREATE TABLE IF NOT EXISTS cfbos_tax_config (
    id BIGSERIAL PRIMARY KEY,
    community_gstin VARCHAR(15),
    community_state_code VARCHAR(2),
    is_gst_registered BOOLEAN NOT NULL DEFAULT FALSE,
    default_gst_rate NUMERIC(5,2) NOT NULL DEFAULT 18.00,
    default_cgst_rate NUMERIC(5,2) NOT NULL DEFAULT 9.00,
    default_sgst_rate NUMERIC(5,2) NOT NULL DEFAULT 9.00,
    financial_year_start_month INTEGER NOT NULL DEFAULT 4,
    updated_by BIGINT,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cfbos_tax_rate (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    tax_type VARCHAR(10) NOT NULL,
    rate NUMERIC(5,2) NOT NULL,
    cgst_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    sgst_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    igst_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cfbos_hsn_sac_code (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    code_type VARCHAR(3) NOT NULL,
    default_gst_rate NUMERIC(5,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cfbos_tds_section (
    id BIGSERIAL PRIMARY KEY,
    section_code VARCHAR(10) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    individual_rate NUMERIC(5,2) NOT NULL,
    company_rate NUMERIC(5,2) NOT NULL,
    threshold_amount NUMERIC(18,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cfbos_tds_rate (
    id BIGSERIAL PRIMARY KEY,
    tds_section_id BIGINT NOT NULL REFERENCES cfbos_tds_section(id),
    payee_type VARCHAR(20) NOT NULL,
    rate NUMERIC(5,2) NOT NULL,
    surcharge NUMERIC(5,2) NOT NULL DEFAULT 0,
    cess NUMERIC(5,2) NOT NULL DEFAULT 0,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Seed default TDS sections
INSERT INTO cfbos_tds_section (section_code, description, individual_rate, company_rate, threshold_amount)
VALUES
    ('194C', 'Payment to Contractors', 1.00, 2.00, 30000.00),
    ('194J', 'Professional/Technical Fees', 10.00, 10.00, 30000.00),
    ('194I', 'Rent Payment', 10.00, 10.00, 240000.00),
    ('194H', 'Commission/Brokerage', 5.00, 5.00, 15000.00),
    ('194A', 'Interest other than Securities', 10.00, 10.00, 40000.00)
ON CONFLICT (section_code) DO NOTHING;

-- Seed default HSN/SAC codes for community services
INSERT INTO cfbos_hsn_sac_code (code, description, code_type, default_gst_rate)
VALUES
    ('9995', 'Maintenance and Repair Services', 'SAC', 18.00),
    ('9972', 'Real Estate Services', 'SAC', 18.00),
    ('9963', 'Accommodation, Food and Beverage Services', 'SAC', 18.00),
    ('9996', 'Recreational, Cultural and Sporting Services', 'SAC', 18.00),
    ('9971', 'Financial and Related Services', 'SAC', 18.00)
ON CONFLICT (code) DO NOTHING;
