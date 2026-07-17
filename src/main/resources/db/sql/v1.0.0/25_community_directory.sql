-- ═══════════════════════════════════════════════════════════════════════════
-- Community Directory: leaders, committee members and contact persons
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS manacommunity.community_leader (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          NOT NULL
        REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    user_id         BIGINT          NOT NULL
        REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    designation     VARCHAR(60)     NOT NULL,
    committee       VARCHAR(60),
    contact_phone   VARCHAR(20),
    contact_email   VARCHAR(120),
    display_order   INT             NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,

    CONSTRAINT uq_community_leader_user_designation
        UNIQUE (community_id, user_id, designation)
);

CREATE INDEX IF NOT EXISTS idx_community_leader_community_active
    ON manacommunity.community_leader (community_id, is_active, display_order);

COMMENT ON TABLE  manacommunity.community_leader IS 'Community leadership directory — executives, committee members and contact persons';
COMMENT ON COLUMN manacommunity.community_leader.designation IS 'Role title, e.g. President, Secretary, Member';
COMMENT ON COLUMN manacommunity.community_leader.committee IS 'Sub-committee name if applicable, NULL for executive roles';
COMMENT ON COLUMN manacommunity.community_leader.display_order IS 'Sort order within the directory — lower numbers appear first';
COMMENT ON COLUMN manacommunity.community_leader.is_active IS 'Soft-delete flag — FALSE hides from public directory';
