-- V69: Create committee_group table for managing named committee groups per community

CREATE TABLE IF NOT EXISTS committee_group (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     TEXT,
    display_order   INT             NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    CONSTRAINT uk_committee_group_community_name UNIQUE (community_id, name)
);

CREATE INDEX IF NOT EXISTS idx_committee_group_community_active
    ON committee_group (community_id, is_active, display_order);
