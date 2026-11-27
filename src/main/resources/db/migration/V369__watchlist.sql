CREATE TYPE watchlist_type AS ENUM ('PERSON','VEHICLE');
CREATE TYPE watchlist_severity AS ENUM ('CAUTION','BLOCKED');

CREATE TABLE watchlist_entry (
    id           BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    type         watchlist_type NOT NULL,
    name         VARCHAR(255),
    identifier   VARCHAR(100) NOT NULL,
    reason       TEXT NOT NULL,
    severity     watchlist_severity NOT NULL DEFAULT 'CAUTION',
    added_by     BIGINT NOT NULL,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at   TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_watchlist_comm       ON watchlist_entry(community_id, is_active);
CREATE INDEX idx_watchlist_identifier ON watchlist_entry(community_id, identifier, is_active);
