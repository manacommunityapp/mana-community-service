-- Smart Water & Electricity IoT Sub-Metering Telemetry Schema

CREATE TABLE IF NOT EXISTS manacommunity.smart_meters (
    id                     BIGSERIAL PRIMARY KEY,
    meter_serial_number    VARCHAR(100)  NOT NULL UNIQUE,
    meter_type             VARCHAR(50)   NOT NULL, -- WATER_METER, ELECTRICITY_METER, GAS_METER, DIESEL_GENERATOR
    community_id           BIGINT        NOT NULL REFERENCES manacommunity.community(id),
    unit_id                BIGINT,
    unit_number            VARCHAR(50),
    block_name             VARCHAR(50),
    protocol               VARCHAR(50)   NOT NULL DEFAULT 'MQTT', -- MQTT, MODBUS_TCP, MODBUS_RTU, HTTP_PULSE
    ip_address             VARCHAR(50),
    mqtt_topic             VARCHAR(200),
    pulse_multiplier       NUMERIC(10,4) NOT NULL DEFAULT 1.0000,
    last_reading           NUMERIC(14,4) NOT NULL DEFAULT 0.0000,
    last_pulse_count       BIGINT        NOT NULL DEFAULT 0,
    last_telemetry_time    TIMESTAMP,
    status                 VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, TAMPERED, OFFLINE, FAULT
    battery_level          INT           DEFAULT 100,
    leak_detected          BOOLEAN       NOT NULL DEFAULT FALSE,
    installed_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_meter_community ON manacommunity.smart_meters(community_id);
CREATE INDEX IF NOT EXISTS idx_meter_unit ON manacommunity.smart_meters(unit_number, block_name);
CREATE INDEX IF NOT EXISTS idx_meter_serial ON manacommunity.smart_meters(meter_serial_number);

CREATE TABLE IF NOT EXISTS manacommunity.meter_telemetry_readings (
    id                     BIGSERIAL PRIMARY KEY,
    meter_id               BIGINT        NOT NULL REFERENCES manacommunity.smart_meters(id) ON DELETE CASCADE,
    timestamp              TIMESTAMP     NOT NULL DEFAULT NOW(),
    raw_pulse_count        BIGINT        NOT NULL,
    cumulative_consumption NUMERIC(14,4) NOT NULL,
    delta_consumption      NUMERIC(14,4) NOT NULL,
    instantaneous_flow     NUMERIC(10,4), -- kW or L/min
    voltage                NUMERIC(8,2),
    current                NUMERIC(8,2),
    power_factor           NUMERIC(4,2),
    tamper_flag            BOOLEAN       NOT NULL DEFAULT FALSE,
    signal_rssi            INT,
    raw_payload_json       TEXT,
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telemetry_meter ON manacommunity.meter_telemetry_readings(meter_id);
CREATE INDEX IF NOT EXISTS idx_telemetry_time ON manacommunity.meter_telemetry_readings(timestamp);

CREATE TABLE IF NOT EXISTS manacommunity.meter_billing_cycle_summaries (
    id                     BIGSERIAL PRIMARY KEY,
    meter_id               BIGINT        NOT NULL REFERENCES manacommunity.smart_meters(id),
    community_id           BIGINT        NOT NULL REFERENCES manacommunity.community(id),
    unit_number            VARCHAR(50)   NOT NULL,
    meter_type             VARCHAR(50)   NOT NULL,
    billing_month          VARCHAR(10)   NOT NULL, -- e.g. '2026-10'
    start_reading          NUMERIC(14,4) NOT NULL,
    end_reading            NUMERIC(14,4) NOT NULL,
    total_units_consumed   NUMERIC(14,4) NOT NULL,
    slab_amount            NUMERIC(12,2) NOT NULL,
    cfbos_invoice_id       BIGINT,
    billing_status         VARCHAR(50)   NOT NULL DEFAULT 'PENDING_SYNC', -- PENDING_SYNC, BILLED_IN_CFBOS, DISPUTED
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_meter_billing_cycle UNIQUE (meter_id, billing_month)
);

CREATE INDEX IF NOT EXISTS idx_meter_bill_month ON manacommunity.meter_billing_cycle_summaries(billing_month);
