-- V358__generator_maintenance.sql
-- Dedicated Diesel Generator (DG) Assets, Digital Run Sheets, 250h B-Checks, Fuel Stock, and AMC Contracts

-- 1. Generator Physical & Operational Assets
CREATE TABLE IF NOT EXISTS manacommunity.generator_assets (
    id                         BIGSERIAL PRIMARY KEY,
    community_id               BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    name                       VARCHAR(100) NOT NULL,
    make_model                 VARCHAR(120) NOT NULL,
    serial_number              VARCHAR(80) NOT NULL,
    capacity_kva               INT NOT NULL,
    location                   VARCHAR(150),
    fuel_tank_capacity_liters  NUMERIC(10,2) NOT NULL DEFAULT 500,
    current_fuel_liters        NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_run_hours            NUMERIC(10,2) NOT NULL DEFAULT 0,
    last_service_hours         NUMERIC(10,2) NOT NULL DEFAULT 0,
    service_interval_hours     INT NOT NULL DEFAULT 250, -- Standard 250-hour B-Check threshold
    battery_voltage            NUMERIC(5,2) DEFAULT 12.6,
    coolant_temp_celsius       INT DEFAULT 65,
    oil_pressure_bar           NUMERIC(4,2) DEFAULT 4.0,
    status                     VARCHAR(30) NOT NULL DEFAULT 'STANDBY', -- STANDBY, RUNNING, MAINTENANCE, FAULT
    amc_vendor                 VARCHAR(150),
    amc_expiry_date            DATE,
    meter_serial_number        VARCHAR(100),
    created_at                 TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gen_serial UNIQUE (serial_number)
);

CREATE INDEX IF NOT EXISTS idx_gen_assets_community ON manacommunity.generator_assets(community_id);
CREATE INDEX IF NOT EXISTS idx_gen_assets_status ON manacommunity.generator_assets(status);

-- 2. Digital Run Sheets & Outage Logs
CREATE TABLE IF NOT EXISTS manacommunity.generator_run_logs (
    id                         BIGSERIAL PRIMARY KEY,
    generator_id               BIGINT NOT NULL REFERENCES manacommunity.generator_assets(id) ON DELETE CASCADE,
    start_time                 TIMESTAMP NOT NULL,
    end_time                   TIMESTAMP NOT NULL,
    duration_hours             NUMERIC(8,2) NOT NULL,
    start_fuel_liters          NUMERIC(10,2) NOT NULL,
    end_fuel_liters            NUMERIC(10,2) NOT NULL,
    diesel_consumed_liters     NUMERIC(10,2) NOT NULL,
    units_generated_kwh        NUMERIC(12,2) NOT NULL,
    fuel_efficiency_kwh_per_l  NUMERIC(6,2),
    reason                     VARCHAR(40) NOT NULL, -- GRID_OUTAGE, SCHEDULED_TEST_RUN, MAINTENANCE_RUN, PEAK_SHAVING
    operator_name              VARCHAR(120),
    notes                      TEXT,
    created_at                 TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gen_runs_generator ON manacommunity.generator_run_logs(generator_id);
CREATE INDEX IF NOT EXISTS idx_gen_runs_time ON manacommunity.generator_run_logs(start_time);

-- 3. Preventive & 250-Hour B-Check Service Records
CREATE TABLE IF NOT EXISTS manacommunity.generator_service_records (
    id                         BIGSERIAL PRIMARY KEY,
    generator_id               BIGINT NOT NULL REFERENCES manacommunity.generator_assets(id) ON DELETE CASCADE,
    service_type               VARCHAR(40) NOT NULL, -- A_CHECK_ROUTINE, B_CHECK_250H, C_CHECK_MAJOR, EMERGENCY_REPAIR, BATTERY_REPLACEMENT
    service_date               DATE NOT NULL,
    run_hours_at_service       NUMERIC(10,2) NOT NULL,
    vendor_name                VARCHAR(150),
    technician_name            VARCHAR(120),
    cost                       NUMERIC(12,2) DEFAULT 0,
    parts_replaced             TEXT, -- Comma-separated list or JSON array of replaced items
    notes                      TEXT,
    status                     VARCHAR(30) NOT NULL DEFAULT 'COMPLETED', -- SCHEDULED, IN_PROGRESS, COMPLETED
    ticket_id                  VARCHAR(50),
    created_at                 TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gen_service_generator ON manacommunity.generator_service_records(generator_id);
CREATE INDEX IF NOT EXISTS idx_gen_service_date ON manacommunity.generator_service_records(service_date);

-- 4. Fuel Refills & Fuel Inventory
CREATE TABLE IF NOT EXISTS manacommunity.generator_fuel_refills (
    id                         BIGSERIAL PRIMARY KEY,
    generator_id               BIGINT NOT NULL REFERENCES manacommunity.generator_assets(id) ON DELETE CASCADE,
    refill_date                DATE NOT NULL,
    quantity_liters            NUMERIC(10,2) NOT NULL,
    price_per_liter            NUMERIC(8,2) NOT NULL,
    total_cost                 NUMERIC(12,2) NOT NULL,
    supplier_name              VARCHAR(150),
    bill_number                VARCHAR(80),
    entered_by                 VARCHAR(120),
    created_at                 TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gen_refills_generator ON manacommunity.generator_fuel_refills(generator_id);
CREATE INDEX IF NOT EXISTS idx_gen_refills_date ON manacommunity.generator_fuel_refills(refill_date);

-- 5. Vendor AMC Agreements & Service Contracts
CREATE TABLE IF NOT EXISTS manacommunity.generator_amc_contracts (
    id                         BIGSERIAL PRIMARY KEY,
    generator_id               BIGINT NOT NULL REFERENCES manacommunity.generator_assets(id) ON DELETE CASCADE,
    vendor_name                VARCHAR(150) NOT NULL,
    contract_number            VARCHAR(80) NOT NULL,
    start_date                 DATE NOT NULL,
    end_date                   DATE NOT NULL,
    annual_cost                NUMERIC(12,2) NOT NULL,
    visits_included            INT NOT NULL DEFAULT 4,
    visits_completed           INT NOT NULL DEFAULT 0,
    emergency_helpline         VARCHAR(100),
    sla_hours                  INT DEFAULT 4,
    status                     VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXPIRING_SOON, EXPIRED
    created_at                 TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gen_amc_generator ON manacommunity.generator_amc_contracts(generator_id);
