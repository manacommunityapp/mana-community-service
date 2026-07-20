-- FILE: db/sql/v1.0.0/34_marketplace_donation_lostfound.sql
-- ═══════════════════════════════════════════════════════════════════════════
-- MARKETPLACE MODULE — Donations & Lost-and-Found
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS donation (
    id              BIGSERIAL       PRIMARY KEY,
    title           VARCHAR(150)    NOT NULL,
    description     VARCHAR(2000),
    category        VARCHAR(40)     NOT NULL,
    condition       VARCHAR(20)     NOT NULL DEFAULT 'GOOD',
    image_url       VARCHAR(500),
    status          VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    donor_id        BIGINT          NOT NULL REFERENCES app_user(id),
    community_id    BIGINT          NOT NULL REFERENCES community(id),
    claimed_by_id   BIGINT          REFERENCES app_user(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lost_and_found (
    id              BIGSERIAL       PRIMARY KEY,
    title           VARCHAR(150)    NOT NULL,
    description     VARCHAR(2000),
    type            VARCHAR(10)     NOT NULL DEFAULT 'LOST',
    category        VARCHAR(40),
    image_url       VARCHAR(500),
    location        VARCHAR(200),
    date_occurred   DATE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    reporter_id     BIGINT          NOT NULL REFERENCES app_user(id),
    community_id    BIGINT          NOT NULL REFERENCES community(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_donation_community ON donation (community_id, status);
CREATE INDEX IF NOT EXISTS idx_lostfound_community ON lost_and_found (community_id, status);
