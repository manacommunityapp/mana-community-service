-- Coach packages
CREATE TABLE IF NOT EXISTS coach_package (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    coach_id        BIGINT NOT NULL,
    sport           VARCHAR(100) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    sessions        INTEGER NOT NULL,
    duration_minutes INTEGER NOT NULL,
    price           NUMERIC(12,2) NOT NULL,
    group_size      INTEGER NOT NULL DEFAULT 1,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS coach_package_enrollment (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    package_id      BIGINT NOT NULL REFERENCES coach_package(id),
    user_id         BIGINT NOT NULL,
    sessions_used   INTEGER NOT NULL DEFAULT 0,
    enrolled_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(package_id, user_id)
);

-- Referee assignments
CREATE TABLE IF NOT EXISTS referee_assignment (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    referee_id      BIGINT NOT NULL,
    event_id        BIGINT NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    sport           VARCHAR(100) NOT NULL,
    assigned_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    confirmed       BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_at    TIMESTAMP,
    fee             NUMERIC(12,2),
    notes           VARCHAR(500)
);

-- Recurring court slot subscriptions
CREATE TABLE IF NOT EXISTS court_slot_subscription (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    court_id        BIGINT NOT NULL,
    day_of_week     INTEGER NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    frequency       VARCHAR(20) NOT NULL DEFAULT 'WEEKLY',
    start_date      DATE NOT NULL,
    end_date        DATE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cp_comm    ON coach_package(community_id, coach_id, active);
CREATE INDEX IF NOT EXISTS idx_cpe_user   ON coach_package_enrollment(user_id, active);
CREATE INDEX IF NOT EXISTS idx_ra_event   ON referee_assignment(event_id, event_type);
CREATE INDEX IF NOT EXISTS idx_css_court  ON court_slot_subscription(court_id, day_of_week, active);
