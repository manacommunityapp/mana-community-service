-- Phase 3: Step Tracking
-- One row per user per day; upserted on each sync/manual log

CREATE TABLE IF NOT EXISTS manacommunity.step_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    log_date        DATE          NOT NULL,
    step_count      INTEGER       NOT NULL DEFAULT 0,
    distance_km     DECIMAL(8,3),
    active_minutes  INTEGER,
    source          VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',  -- MANUAL | HEALTH_CONNECT | WEARABLE
    device_id       BIGINT        REFERENCES manacommunity.wearable_device(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_step_log_user_date UNIQUE (user_id, log_date)
);

CREATE INDEX IF NOT EXISTS idx_step_log_user_date ON manacommunity.step_log (user_id, log_date DESC);
