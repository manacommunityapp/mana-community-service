-- Home Services: photos, quality checks, warranty, ratings, payouts, recurring plans
CREATE TYPE hs_photo_stage AS ENUM ('PRE_WORK','POST_WORK','ISSUE');
CREATE TYPE hs_warranty_status AS ENUM ('ACTIVE','CLAIMED','EXPIRED','VOID');
CREATE TYPE hs_rework_status AS ENUM ('REQUESTED','APPROVED','SCHEDULED','COMPLETED','REJECTED');
CREATE TYPE hs_plan_frequency AS ENUM ('WEEKLY','FORTNIGHTLY','MONTHLY','QUARTERLY');
CREATE TYPE hs_plan_status AS ENUM ('ACTIVE','PAUSED','CANCELLED');

CREATE TABLE hs_booking_photo (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT NOT NULL REFERENCES hs_booking(id) ON DELETE CASCADE,
    stage           hs_photo_stage NOT NULL,
    photo_url       VARCHAR(1024) NOT NULL,
    uploaded_by     BIGINT NOT NULL,
    caption         VARCHAR(500),
    uploaded_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE hs_quality_check (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT NOT NULL REFERENCES hs_booking(id),
    checked_by      BIGINT NOT NULL,
    overall_score   INT NOT NULL CHECK (overall_score BETWEEN 1 AND 5),
    cleanliness     INT CHECK (cleanliness BETWEEN 1 AND 5),
    timeliness      INT CHECK (timeliness BETWEEN 1 AND 5),
    workmanship     INT CHECK (workmanship BETWEEN 1 AND 5),
    notes           TEXT,
    checked_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(booking_id)
);

CREATE TABLE hs_warranty (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT NOT NULL REFERENCES hs_booking(id),
    community_id    BIGINT NOT NULL,
    warranty_days   INT NOT NULL DEFAULT 30,
    expires_at      DATE NOT NULL,
    status          hs_warranty_status NOT NULL DEFAULT 'ACTIVE',
    claim_reason    TEXT,
    claimed_at      TIMESTAMP,
    UNIQUE(booking_id)
);

CREATE TABLE hs_rework_request (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT NOT NULL REFERENCES hs_booking(id),
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    reason          TEXT NOT NULL,
    status          hs_rework_status NOT NULL DEFAULT 'REQUESTED',
    rescheduled_at  TIMESTAMP,
    resolved_at     TIMESTAMP,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE hs_booking_rating (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT NOT NULL REFERENCES hs_booking(id),
    user_id         BIGINT NOT NULL,
    worker_id       BIGINT NOT NULL,
    overall         INT NOT NULL CHECK (overall BETWEEN 1 AND 5),
    punctuality     INT CHECK (punctuality BETWEEN 1 AND 5),
    quality         INT CHECK (quality BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(booking_id)
);

CREATE TABLE hs_worker_payout (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    worker_id       BIGINT NOT NULL REFERENCES hs_worker(id),
    booking_id      BIGINT REFERENCES hs_booking(id),
    amount          NUMERIC(12,2) NOT NULL,
    deductions      NUMERIC(12,2) NOT NULL DEFAULT 0,
    net_amount      NUMERIC(12,2) NOT NULL,
    period_start    DATE,
    period_end      DATE,
    paid            BOOLEAN NOT NULL DEFAULT FALSE,
    paid_at         TIMESTAMP,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE hs_recurring_plan (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    package_id      BIGINT REFERENCES hs_service_package(id),
    category        hs_service_category NOT NULL,
    frequency       hs_plan_frequency NOT NULL,
    next_service_date DATE NOT NULL,
    preferred_time  TIME,
    status          hs_plan_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hsbp_booking  ON hs_booking_photo(booking_id, stage);
CREATE INDEX idx_hswp_worker   ON hs_worker_payout(worker_id, paid);
CREATE INDEX idx_hsrp_user     ON hs_recurring_plan(user_id, status);
CREATE INDEX idx_hsrp_next     ON hs_recurring_plan(next_service_date, status) WHERE status = 'ACTIVE';
