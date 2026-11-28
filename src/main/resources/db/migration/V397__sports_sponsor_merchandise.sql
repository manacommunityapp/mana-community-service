-- Tournament sponsors
CREATE TABLE IF NOT EXISTS tournament_sponsor (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    tournament_id   BIGINT NOT NULL,
    sponsor_name    VARCHAR(255) NOT NULL,
    tier            VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    amount          NUMERIC(12,2) NOT NULL DEFAULT 0,
    logo_key        VARCHAR(500),
    benefits        TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    contact_name    VARCHAR(255),
    contact_email   VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tournament merchandise
CREATE TABLE IF NOT EXISTS tournament_merchandise (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    tournament_id   BIGINT NOT NULL,
    item_name       VARCHAR(255) NOT NULL,
    description     TEXT,
    price           NUMERIC(12,2) NOT NULL,
    stock_qty       INTEGER NOT NULL DEFAULT 0,
    image_key       VARCHAR(500),
    is_available    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS merchandise_order (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    merchandise_id  BIGINT NOT NULL REFERENCES tournament_merchandise(id),
    user_id         BIGINT NOT NULL,
    quantity        INTEGER NOT NULL DEFAULT 1,
    unit_price      NUMERIC(12,2) NOT NULL,
    total_amount    NUMERIC(12,2) NOT NULL,
    payment_ref     VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ts_tournament ON tournament_sponsor(tournament_id, status);
CREATE INDEX IF NOT EXISTS idx_tm_tournament ON tournament_merchandise(tournament_id, is_available);
CREATE INDEX IF NOT EXISTS idx_mo_user       ON merchandise_order(user_id);
CREATE INDEX IF NOT EXISTS idx_mo_merch      ON merchandise_order(merchandise_id);
