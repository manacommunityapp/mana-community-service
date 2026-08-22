-- Migration V63: Create community_who_to_call and community_who_to_call_history tables

CREATE TABLE IF NOT EXISTS community_who_to_call (
    id                  BIGSERIAL       PRIMARY KEY,
    community_id        BIGINT          NOT NULL,
    department          VARCHAR(100)    NOT NULL,
    contact_person      VARCHAR(100)    NOT NULL,
    user_id             BIGINT,
    phone_number        VARCHAR(25)     NOT NULL,
    secondary_phone     VARCHAR(25),
    email               VARCHAR(120),
    designation         VARCHAR(100),
    availability        VARCHAR(100),
    location_or_desk    VARCHAR(100),
    icon                VARCHAR(50)     NOT NULL DEFAULT 'HelpCircle',
    color               VARCHAR(100)    NOT NULL DEFAULT 'text-indigo-600 bg-indigo-50 border-indigo-200',
    is_emergency        BOOLEAN         NOT NULL DEFAULT FALSE,
    display_order       INT             NOT NULL DEFAULT 0,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT
);

CREATE INDEX IF NOT EXISTS idx_who_to_call_community_active
    ON community_who_to_call (community_id, is_active, display_order);

CREATE TABLE IF NOT EXISTS community_who_to_call_history (
    id                  BIGSERIAL       PRIMARY KEY,
    who_to_call_id      BIGINT          NOT NULL,
    community_id        BIGINT          NOT NULL,
    action              VARCHAR(30)     NOT NULL,
    changed_by_user_id  BIGINT,
    changed_by_name     VARCHAR(100),
    department          VARCHAR(100)    NOT NULL,
    contact_person      VARCHAR(100)    NOT NULL,
    phone_number        VARCHAR(25)     NOT NULL,
    change_summary      TEXT,
    snapshot_data       TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_who_to_call_hist_community
    ON community_who_to_call_history (community_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_who_to_call_hist_item
    ON community_who_to_call_history (who_to_call_id, created_at DESC);
