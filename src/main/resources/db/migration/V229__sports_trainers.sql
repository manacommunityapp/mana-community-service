-- ──────────────────────────────────────────────────────────────────────────────
-- V229: Sports Trainers module
--   Tables: sports_trainer, trainer_slot, trainer_slot_booking
-- ──────────────────────────────────────────────────────────────────────────────

-- ── 1. Sports Trainers ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sports_trainer (
    id                 BIGSERIAL     PRIMARY KEY,
    community_id       BIGINT        NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    user_id            BIGINT        NOT NULL,
    full_name          VARCHAR(150)  NOT NULL,
    email              VARCHAR(200),
    phone              VARCHAR(20),
    sport              VARCHAR(100)  NOT NULL,
    cert_body          VARCHAR(150),
    cert_number        VARCHAR(100),
    cert_year          INT,
    age_groups_coached VARCHAR(100),
    bio                VARCHAR(1000),
    experience_years   INT,
    profile_pic_url    VARCHAR(500),
    status             VARCHAR(30)   NOT NULL DEFAULT 'APPLIED',
    approved_by        BIGINT,
    approved_at        TIMESTAMP,
    rejection_reason   VARCHAR(500),
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sports_trainer_community ON sports_trainer (community_id);
CREATE INDEX IF NOT EXISTS idx_sports_trainer_user      ON sports_trainer (user_id);
CREATE INDEX IF NOT EXISTS idx_sports_trainer_sport     ON sports_trainer (sport);
CREATE INDEX IF NOT EXISTS idx_sports_trainer_status    ON sports_trainer (status);

-- ── 2. Trainer Slots ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS trainer_slot (
    id               BIGSERIAL     PRIMARY KEY,
    trainer_id       BIGINT        NOT NULL REFERENCES sports_trainer(id) ON DELETE CASCADE,
    sport            VARCHAR(100)  NOT NULL,
    slot_date        DATE          NOT NULL,
    start_time       TIME          NOT NULL,
    end_time         TIME          NOT NULL,
    duration_minutes INT           NOT NULL,
    slot_type        VARCHAR(20)   NOT NULL DEFAULT 'INDIVIDUAL',
    max_capacity     INT           NOT NULL DEFAULT 1,
    booked_count     INT           NOT NULL DEFAULT 0,
    venue_id         BIGINT,
    venue_name       VARCHAR(150),
    price            NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    status           VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    notes            VARCHAR(500),
    community_id     BIGINT        NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_trainer_slot_trainer ON trainer_slot (trainer_id);
CREATE INDEX IF NOT EXISTS idx_trainer_slot_date    ON trainer_slot (slot_date);
CREATE INDEX IF NOT EXISTS idx_trainer_slot_sport   ON trainer_slot (sport);
CREATE INDEX IF NOT EXISTS idx_trainer_slot_status  ON trainer_slot (status);

-- ── 3. Trainer Slot Bookings ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS trainer_slot_booking (
    id           BIGSERIAL     PRIMARY KEY,
    slot_id      BIGINT        NOT NULL REFERENCES trainer_slot(id) ON DELETE CASCADE,
    trainer_id   BIGINT        NOT NULL,
    user_id      BIGINT        NOT NULL,
    user_name    VARCHAR(150)  NOT NULL,
    tower        VARCHAR(20),
    flat_number  VARCHAR(20),
    status       VARCHAR(20)   NOT NULL DEFAULT 'CONFIRMED',
    amount_paid  NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    community_id BIGINT        NOT NULL,
    booked_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tsb_slot    ON trainer_slot_booking (slot_id);
CREATE INDEX IF NOT EXISTS idx_tsb_user    ON trainer_slot_booking (user_id);
CREATE INDEX IF NOT EXISTS idx_tsb_trainer ON trainer_slot_booking (trainer_id);
CREATE INDEX IF NOT EXISTS idx_tsb_status  ON trainer_slot_booking (status);
