-- ============================================================================
-- V516: Truein Parity: Contractor Workforce Management (CWMS) & AI TimeGuard
-- ============================================================================

-- 1. Contractor Agencies Table
CREATE TABLE IF NOT EXISTS hr_contractor_agency (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    agency_name VARCHAR(150) NOT NULL,
    agency_code VARCHAR(50) UNIQUE NOT NULL,
    contact_person VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    gst_number VARCHAR(30),
    billing_rate_per_hour NUMERIC(10, 2) DEFAULT 180.00,
    max_headcount_quota INT DEFAULT 20,
    allocated_gates TEXT[],
    service_categories TEXT[],
    compliance_status VARCHAR(30) DEFAULT 'COMPLIANT',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_hr_contractor_agency_org ON hr_contractor_agency(organization_id);
CREATE INDEX IF NOT EXISTS idx_hr_contractor_agency_comm ON hr_contractor_agency(community_id);

-- 2. Contractor Workers Table
CREATE TABLE IF NOT EXISTS hr_contractor_worker (
    id BIGSERIAL PRIMARY KEY,
    agency_id BIGINT REFERENCES hr_contractor_agency(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL,
    worker_code VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20),
    category VARCHAR(60),
    deployed_site VARCHAR(120),
    shift_name VARCHAR(60),
    face_embedding_hash VARCHAR(256),
    kyc_verified BOOLEAN DEFAULT FALSE,
    dpdp_consent_signed BOOLEAN DEFAULT FALSE,
    safety_training_passed BOOLEAN DEFAULT FALSE,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    last_punch_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_hr_contractor_worker_agency ON hr_contractor_worker(agency_id);
CREATE INDEX IF NOT EXISTS idx_hr_contractor_worker_code ON hr_contractor_worker(worker_code);

-- 3. AI TimeGuard Anomalies Table
CREATE TABLE IF NOT EXISTS hr_timeguard_anomaly (
    id VARCHAR(64) PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    community_id BIGINT NOT NULL,
    anomaly_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    person_name VARCHAR(120),
    person_role VARCHAR(60),
    agency_name VARCHAR(150),
    gate_location VARCHAR(100) NOT NULL,
    confidence_score NUMERIC(5, 4),
    forensic_details TEXT,
    status VARCHAR(30) DEFAULT 'FLAGGED',
    action_taken TEXT,
    event_timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_hr_timeguard_org ON hr_timeguard_anomaly(organization_id);
CREATE INDEX IF NOT EXISTS idx_hr_timeguard_status ON hr_timeguard_anomaly(status);

-- 4. Turnstile Offline Punch Idempotency Cache
CREATE TABLE IF NOT EXISTS hr_turnstile_offline_punch (
    punch_uuid VARCHAR(64) PRIMARY KEY,
    turnstile_identifier VARCHAR(60) NOT NULL,
    face_embedding_hash VARCHAR(256) NOT NULL,
    punch_type VARCHAR(30) NOT NULL,
    confidence_score NUMERIC(5, 4),
    device_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    decision VARCHAR(30) NOT NULL
);
