-- V359__water_maintenance.sql
-- Dedicated Water Maintenance: Storage Sumps & OHTs, Tanker Deliveries, Pump Sets, WTP/STP Logs, Quality Tests, and Tank Cleaning Schedules

-- 1. Storage Tanks & Sumps Telemetry
CREATE TABLE IF NOT EXISTS manacommunity.water_storage_tanks (
    id                          BIGSERIAL PRIMARY KEY,
    community_id                BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    name                        VARCHAR(120) NOT NULL,
    tank_type                   VARCHAR(50) NOT NULL, -- UG_RAW, UG_TREATED, UG_SOFT, UG_FIRE_RESERVE, OHT_TOWER_A, OHT_TOWER_B, OHT_TOWER_C
    location                    VARCHAR(150),
    capacity_liters             NUMERIC(12,2) NOT NULL DEFAULT 100000,
    current_level_liters        NUMERIC(12,2) NOT NULL DEFAULT 50000,
    low_level_threshold_liters  NUMERIC(12,2) NOT NULL DEFAULT 25000,
    sensor_status               VARCHAR(30) NOT NULL DEFAULT 'ONLINE', -- ONLINE, OFFLINE, CALIBRATING
    last_telemetry_time         TIMESTAMP,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_water_tanks_community ON manacommunity.water_storage_tanks(community_id);
CREATE INDEX IF NOT EXISTS idx_water_tanks_type ON manacommunity.water_storage_tanks(tank_type);

-- 2. Water Tanker Procurement & Inwarding
CREATE TABLE IF NOT EXISTS manacommunity.water_tanker_deliveries (
    id                          BIGSERIAL PRIMARY KEY,
    community_id                BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    delivery_date               TIMESTAMP NOT NULL DEFAULT NOW(),
    vendor_name                 VARCHAR(150) NOT NULL,
    vehicle_number              VARCHAR(50) NOT NULL,
    capacity_liters             NUMERIC(10,2) NOT NULL DEFAULT 12000,
    cost                        NUMERIC(10,2) NOT NULL DEFAULT 1200,
    invoice_number              VARCHAR(100),
    driver_phone                VARCHAR(50),
    target_tank_id              BIGINT REFERENCES manacommunity.water_storage_tanks(id) ON DELETE SET NULL,
    approved_by                 VARCHAR(120),
    status                      VARCHAR(30) NOT NULL DEFAULT 'RECEIVED', -- RECEIVED, PENDING_VERIFICATION, REJECTED
    notes                       TEXT,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_water_tankers_community ON manacommunity.water_tanker_deliveries(community_id);
CREATE INDEX IF NOT EXISTS idx_water_tankers_date ON manacommunity.water_tanker_deliveries(delivery_date);

-- 3. Pump House & HYPN Motors
CREATE TABLE IF NOT EXISTS manacommunity.water_pumps (
    id                          BIGSERIAL PRIMARY KEY,
    community_id                BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    name                        VARCHAR(120) NOT NULL,
    pump_type                   VARCHAR(50) NOT NULL, -- TRANSFER_PUMP, BOOSTER_PUMP, BOREWELL_PUMP, DRAINAGE_PUMP, FIRE_HYDRANT
    location                    VARCHAR(150),
    rated_kw                    NUMERIC(6,2) NOT NULL DEFAULT 7.5,
    duty_cycle_hours            INT NOT NULL DEFAULT 4,
    total_run_hours             NUMERIC(10,2) NOT NULL DEFAULT 0,
    status                      VARCHAR(30) NOT NULL DEFAULT 'STANDBY', -- RUNNING, STANDBY, MAINTENANCE, FAULT
    is_duty_pump                BOOLEAN NOT NULL DEFAULT FALSE,
    motor_health_rating         VARCHAR(30) DEFAULT 'GOOD', -- EXCELLENT, GOOD, ATTENTION_NEEDED, CRITICAL
    last_service_date           DATE,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_water_pumps_community ON manacommunity.water_pumps(community_id);
CREATE INDEX IF NOT EXISTS idx_water_pumps_status ON manacommunity.water_pumps(status);

-- 4. Pump House Run & Operational Logs
CREATE TABLE IF NOT EXISTS manacommunity.water_pump_run_logs (
    id                          BIGSERIAL PRIMARY KEY,
    pump_id                     BIGINT NOT NULL REFERENCES manacommunity.water_pumps(id) ON DELETE CASCADE,
    start_time                  TIMESTAMP NOT NULL,
    end_time                    TIMESTAMP NOT NULL,
    duration_hours              NUMERIC(8,2) NOT NULL,
    flow_rate_lpm               NUMERIC(8,2),
    energy_kwh                  NUMERIC(8,2),
    operator_name               VARCHAR(120),
    notes                       TEXT,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_water_pump_logs_pump ON manacommunity.water_pump_run_logs(pump_id);

-- 5. WTP & STP Treatment Operations
CREATE TABLE IF NOT EXISTS manacommunity.water_treatment_logs (
    id                          BIGSERIAL PRIMARY KEY,
    community_id                BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    plant_type                  VARCHAR(30) NOT NULL, -- WTP, STP
    operation_type              VARCHAR(50) NOT NULL, -- MGF_BACKWASH, ACF_BACKWASH, SOFTENER_SALT_RECHARGE, STP_AERATION_CHECK, CHLORINE_DOSING, SLUDGE_DISPOSAL
    log_date                    TIMESTAMP NOT NULL DEFAULT NOW(),
    quantity_used_kg            NUMERIC(8,2) DEFAULT 0,
    operator_name               VARCHAR(120),
    notes                       TEXT,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_treatment_logs_community ON manacommunity.water_treatment_logs(community_id);
CREATE INDEX IF NOT EXISTS idx_treatment_logs_date ON manacommunity.water_treatment_logs(log_date);

-- 6. Water Quality Testing & Hygiene Compliance
CREATE TABLE IF NOT EXISTS manacommunity.water_quality_tests (
    id                          BIGSERIAL PRIMARY KEY,
    community_id                BIGINT NOT NULL REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    test_date                   TIMESTAMP NOT NULL DEFAULT NOW(),
    source_location             VARCHAR(80) NOT NULL, -- MUNICIPAL_INFLOW, BOREWELL_1, WTP_OUTLET, STP_TREATED, TOWER_A_TAP
    ph                          NUMERIC(4,2) NOT NULL,
    tds_ppm                     INT NOT NULL,
    hardness_ppm                INT NOT NULL,
    residual_chlorine_ppm       NUMERIC(4,2),
    turbidity_ntu               NUMERIC(4,2),
    status                      VARCHAR(30) NOT NULL DEFAULT 'SAFE', -- SAFE, MARGINAL, CONTAMINATED
    tested_by                   VARCHAR(120),
    lab_report_url              VARCHAR(255),
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quality_tests_community ON manacommunity.water_quality_tests(community_id);
CREATE INDEX IF NOT EXISTS idx_quality_tests_date ON manacommunity.water_quality_tests(test_date);

-- 7. Mechanized Sump & Tank Cleaning Schedules
CREATE TABLE IF NOT EXISTS manacommunity.tank_cleaning_schedules (
    id                          BIGSERIAL PRIMARY KEY,
    tank_id                     BIGINT NOT NULL REFERENCES manacommunity.water_storage_tanks(id) ON DELETE CASCADE,
    last_cleaned_date           DATE NOT NULL,
    next_due_date               DATE NOT NULL,
    cleaning_type               VARCHAR(60) NOT NULL DEFAULT 'MECHANIZED_DEWATERING_UV', -- MECHANIZED_DEWATERING_UV, CHEMICAL_DISINFECTION, MANUAL_SCRUB
    vendor_name                 VARCHAR(150),
    cost                        NUMERIC(10,2) DEFAULT 0,
    status                      VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, COMPLETED, OVERDUE
    certificate_url             VARCHAR(255),
    notes                       TEXT,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cleaning_schedules_tank ON manacommunity.tank_cleaning_schedules(tank_id);
