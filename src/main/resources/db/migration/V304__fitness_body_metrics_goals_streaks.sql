-- ══════════════════════════════════════════════════════════════
-- V239  Fitness Phase 2 — body_metric_log, activity_goal, fitness_streak
-- ══════════════════════════════════════════════════════════════

SET search_path = manacommunity;

-- ── Body metric history (one row per weigh-in / measurement) ─
CREATE TABLE body_metric_log (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    logged_date       DATE NOT NULL DEFAULT CURRENT_DATE,
    weight_kg         NUMERIC(5,1),
    body_fat_pct      NUMERIC(4,1),
    muscle_mass_kg    NUMERIC(5,1),
    bmi               NUMERIC(4,1),         -- computed from weight / height²
    waist_cm          NUMERIC(5,1),
    chest_cm          NUMERIC(5,1),
    hip_cm            NUMERIC(5,1),
    source            VARCHAR(20) NOT NULL DEFAULT 'MANUAL',  -- MANUAL / SMART_SCALE / WEARABLE
    notes             VARCHAR(300),
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_body_metric_user_date ON body_metric_log(user_id, logged_date DESC);
CREATE UNIQUE INDEX idx_body_metric_user_date_unique ON body_metric_log(user_id, logged_date);

-- ── Activity goal (one record per user, upserted) ─────────────
CREATE TABLE activity_goal (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT NOT NULL UNIQUE,
    daily_steps_target          INTEGER NOT NULL DEFAULT 8000,
    active_minutes_target       INTEGER NOT NULL DEFAULT 30,
    weekly_workouts_target      INTEGER NOT NULL DEFAULT 4,
    created_at                  TIMESTAMP DEFAULT NOW(),
    updated_at                  TIMESTAMP DEFAULT NOW()
);

-- ── Fitness streak (one record per user, updated on each workout log) ─
CREATE TABLE fitness_streak (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL UNIQUE,
    current_streak_days     INTEGER NOT NULL DEFAULT 0,
    longest_streak_days     INTEGER NOT NULL DEFAULT 0,
    last_activity_date      DATE,
    total_active_days       INTEGER NOT NULL DEFAULT 0,
    updated_at              TIMESTAMP DEFAULT NOW()
);
