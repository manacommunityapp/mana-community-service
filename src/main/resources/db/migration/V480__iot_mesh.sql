-- IoT mesh: environmental/non-utility sensors, anomaly alert configs, and triggered events
-- Note: utility metering (water/electricity/gas) already handled by smart_meters (V146).
-- This extends the IoT estate to environmental, structural, and emergency sensors.

CREATE TABLE iot_sensors (
  id                       BIGSERIAL PRIMARY KEY,
  community_id             BIGINT NOT NULL,
  sensor_name              VARCHAR(200) NOT NULL,
  sensor_type              VARCHAR(50)  NOT NULL,
                           -- TEMPERATURE, HUMIDITY, SMOKE_DETECTOR, FLOOD_SENSOR,
                           -- GENERATOR_STATUS, LIFT_STATUS, SOLAR_INVERTER,
                           -- AIR_QUALITY, CO_DETECTOR, MOTION_SENSOR
  location                 VARCHAR(200) NOT NULL,
  unit                     VARCHAR(20),            -- °C, %, ppm, boolean
  vendor                   VARCHAR(100),
  serial_number            VARCHAR(100),
  api_endpoint             VARCHAR(500),
  api_key_ref              VARCHAR(200),           -- reference to env secret, never the key itself
  polling_interval_seconds INT NOT NULL DEFAULT 300,
  is_active                BOOLEAN NOT NULL DEFAULT TRUE,
  last_reading             NUMERIC(15,4),
  last_reading_at          TIMESTAMPTZ,
  created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE sensor_telemetry (
  id            BIGSERIAL,
  sensor_id     BIGINT NOT NULL,
  community_id  BIGINT NOT NULL,
  reading_value NUMERIC(15,4) NOT NULL,
  reading_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  quality       VARCHAR(20) NOT NULL DEFAULT 'GOOD'  -- GOOD, ESTIMATED, BAD
) PARTITION BY RANGE (reading_at);

CREATE TABLE sensor_telemetry_2025_11 PARTITION OF sensor_telemetry
  FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE sensor_telemetry_2025_12 PARTITION OF sensor_telemetry
  FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');
CREATE TABLE sensor_telemetry_2026_01 PARTITION OF sensor_telemetry
  FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE sensor_telemetry_2026_02 PARTITION OF sensor_telemetry
  FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE sensor_telemetry_2026_03 PARTITION OF sensor_telemetry
  FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE sensor_telemetry_default PARTITION OF sensor_telemetry DEFAULT;

CREATE TABLE sensor_alert_configs (
  id               BIGSERIAL PRIMARY KEY,
  sensor_id        BIGINT NOT NULL REFERENCES iot_sensors(id),
  alert_type       VARCHAR(50) NOT NULL,
                   -- THRESHOLD_HIGH, THRESHOLD_LOW, RATE_CHANGE, OFFLINE
  threshold_value  NUMERIC(15,4),
  rate_change_pct  NUMERIC(5,2),
  window_minutes   INT NOT NULL DEFAULT 60,
  severity         VARCHAR(20) NOT NULL DEFAULT 'WARNING', -- WARNING, CRITICAL
  notify_roles     VARCHAR(200) NOT NULL DEFAULT 'FM,ADMIN',
  is_active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE sensor_anomaly_events (
  id               BIGSERIAL PRIMARY KEY,
  sensor_id        BIGINT NOT NULL REFERENCES iot_sensors(id),
  community_id     BIGINT NOT NULL,
  alert_config_id  BIGINT REFERENCES sensor_alert_configs(id),
  event_type       VARCHAR(50) NOT NULL,
  detected_value   NUMERIC(15,4),
  threshold_value  NUMERIC(15,4),
  severity         VARCHAR(20) NOT NULL,
  description      TEXT,
  resolved_at      TIMESTAMPTZ,
  resolved_by      BIGINT,
  detected_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_iot_sensors_community ON iot_sensors(community_id, sensor_type);
CREATE INDEX idx_telemetry_sensor ON sensor_telemetry(sensor_id, reading_at DESC);
CREATE INDEX idx_anomaly_community_open ON sensor_anomaly_events(community_id, severity, detected_at DESC)
  WHERE resolved_at IS NULL;
