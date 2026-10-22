-- Phase 4: Wearable Device Integration

CREATE TABLE IF NOT EXISTS manacommunity.wearable_device (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL,
    device_type         VARCHAR(40)   NOT NULL DEFAULT 'GENERIC_BLUETOOTH',
    device_identifier   VARCHAR(120)  NOT NULL,
    device_name         VARCHAR(80),
    firmware_version    VARCHAR(20),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    last_sync_at        TIMESTAMP,
    registered_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wearable_user_device UNIQUE (user_id, device_identifier)
);

CREATE INDEX idx_wearable_device_user ON manacommunity.wearable_device (user_id);

CREATE TABLE IF NOT EXISTS manacommunity.wearable_health_event (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL,
    device_id           BIGINT,
    event_type          VARCHAR(40)   NOT NULL,
    heart_rate_bpm      INTEGER,
    spo2_percent        NUMERIC(5, 2),
    latitude            NUMERIC(9, 6),
    longitude           NUMERIC(9, 6),
    raw_data            TEXT,
    sos_triggered       BOOLEAN       NOT NULL DEFAULT FALSE,
    linked_incident_id  BIGINT,
    event_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wearable_event_user_time ON manacommunity.wearable_health_event (user_id, event_at DESC);
CREATE INDEX idx_wearable_event_type      ON manacommunity.wearable_health_event (event_type);
