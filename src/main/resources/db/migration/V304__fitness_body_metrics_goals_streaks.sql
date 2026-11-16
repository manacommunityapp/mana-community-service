-- V239 Fitness Phase 2 - body_metric_log, activity_goal, fitness_streak (renamed; may already exist)
SET search_path = manacommunity;

CREATE TABLE IF NOT EXISTS body_metric_log (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    logged_date       DATE NOT NULL DEFAULT CURRENT_DATE,
    weight_kg         NUMERIC(5,1),
    body_fat_pct      NUMERIC(4,1),
    muscle_mass_kg    NUMERIC(5,1),
    bmi               NUMERIC(4,1),
    waist_cm          NUMERIC(5,1),
    chest_cm          NUMERIC(5,1),
    hip_cm            NUMERIC(5,1),
    source            VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    notes             VARCHAR(300),
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_body_metric_user_date ON body_metric_log(user_id, logged_date DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_body_metric_user_date_unique ON body_metric_log(user_id, logged_date);

CREATE TABLE IF NOT EXISTS activity_goal (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT NOT NULL UNIQUE,
    daily_steps_target          INTEGER NOT NULL DEFAULT 8000,
    active_minutes_target       INTEGER NOT NULL DEFAULT 30,
    weekly_workouts_target      INTEGER NOT NULL DEFAULT 4,
    created_at                  TIMESTAMP DEFAULT NOW(),
    updated_at                  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fitness_streak (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL UNIQUE,
    current_streak_days     INTEGER NOT NULL DEFAULT 0,
    longest_streak_days     INTEGER NOT NULL DEFAULT 0,
    last_activity_date      DATE,
    total_active_days       INTEGER NOT NULL DEFAULT 0,
    updated_at              TIMESTAMP DEFAULT NOW()
);
