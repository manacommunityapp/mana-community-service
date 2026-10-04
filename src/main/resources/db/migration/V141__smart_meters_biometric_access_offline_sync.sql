-- Smart Meters
CREATE TABLE IF NOT EXISTS smart_meters (
    id BIGSERIAL PRIMARY KEY,
    society_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL,
    unit_number VARCHAR(50),
    meter_number VARCHAR(50) NOT NULL UNIQUE,
    meter_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_reading DOUBLE PRECISION DEFAULT 0,
    unit_of_measure VARCHAR(10) NOT NULL,
    pulse_multiplier DOUBLE PRECISION DEFAULT 1.0,
    last_reading_at TIMESTAMP,
    burst_leak_detected BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS utility_consumption_summary (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    unit_number VARCHAR(50),
    cycle_month VARCHAR(7) NOT NULL,
    electricity_kwh DOUBLE PRECISION DEFAULT 0,
    electricity_amount NUMERIC(10, 2) DEFAULT 0,
    water_liters DOUBLE PRECISION DEFAULT 0,
    water_amount NUMERIC(10, 2) DEFAULT 0,
    dg_backup_kwh DOUBLE PRECISION DEFAULT 0,
    dg_backup_amount NUMERIC(10, 2) DEFAULT 0,
    total_utility_amount NUMERIC(10, 2) DEFAULT 0,
    cfbos_sync_status VARCHAR(10) DEFAULT 'PENDING'
);

CREATE INDEX IF NOT EXISTS idx_utility_unit_month ON utility_consumption_summary(unit_id, cycle_month);

-- Biometric Access
CREATE TABLE IF NOT EXISTS turnstiles (
    id BIGSERIAL PRIMARY KEY,
    society_id BIGINT NOT NULL,
    turnstile_code VARCHAR(50) NOT NULL UNIQUE,
    turnstile_name VARCHAR(150),
    gate_location VARCHAR(200),
    turnstile_type VARCHAR(30) NOT NULL,
    ip_address VARCHAR(45),
    relay_pin INT,
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    confidence_threshold DOUBLE PRECISION DEFAULT 0.80,
    unlock_duration_seconds INT DEFAULT 3,
    last_heartbeat TIMESTAMP
);

CREATE TABLE IF NOT EXISTS access_log (
    id BIGSERIAL PRIMARY KEY,
    society_id BIGINT NOT NULL,
    turnstile_code VARCHAR(50) NOT NULL,
    turnstile_name VARCHAR(150),
    decision VARCHAR(30) NOT NULL,
    user_id BIGINT,
    user_full_name VARCHAR(150),
    user_type VARCHAR(20),
    confidence_score DOUBLE PRECISION,
    reason VARCHAR(255),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_access_log_society_ts ON access_log(society_id, timestamp DESC);

CREATE TABLE IF NOT EXISTS staff_schedule_rules (
    id BIGSERIAL PRIMARY KEY,
    society_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    staff_name VARCHAR(150),
    role VARCHAR(30),
    allowed_start_time VARCHAR(10),
    allowed_end_time VARCHAR(10),
    allowed_days_of_week TEXT,
    is_active BOOLEAN DEFAULT TRUE
);

-- Offline Sync
CREATE TABLE IF NOT EXISTS sync_change_log (
    id BIGSERIAL PRIMARY KEY,
    society_id BIGINT NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    action VARCHAR(10) NOT NULL,
    entity_id BIGINT NOT NULL,
    payload_json TEXT,
    server_vector_timestamp BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sync_society_ts ON sync_change_log(society_id, server_vector_timestamp);

CREATE TABLE IF NOT EXISTS sync_mutations (
    id BIGSERIAL PRIMARY KEY,
    mutation_id VARCHAR(50) NOT NULL UNIQUE,
    society_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    action VARCHAR(10) NOT NULL,
    payload_json TEXT,
    vector_timestamp BIGINT NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
