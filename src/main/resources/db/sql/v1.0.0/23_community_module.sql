-- Community module permission table
-- Stores which feature modules are enabled per community.
-- Required for prod (ddl-auto: validate); local=create / dev=create auto-create it.

CREATE TABLE IF NOT EXISTS community_module (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          NOT NULL REFERENCES community(id),
    module_key      VARCHAR(50)     NOT NULL,
    module_label    VARCHAR(100)    NOT NULL,
    is_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order      INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    CONSTRAINT uq_community_module_key UNIQUE (community_id, module_key)
);

CREATE INDEX IF NOT EXISTS idx_cm_community ON community_module(community_id);

-- Remove the old comma-separated column from community table
ALTER TABLE community DROP COLUMN IF EXISTS enabled_modules;

-- Seed default modules for all existing communities
INSERT INTO community_module (community_id, module_key, module_label, is_enabled, sort_order, created_at)
SELECT c.id, m.module_key, m.module_label, TRUE, m.sort_order, NOW()
FROM community c
CROSS JOIN (VALUES
    ('COMMUNITY_FEED',  'Community Feed',    1),
    ('SPORTS',          'Sports',            2),
    ('MARKETPLACE',     'Marketplace',       3),
    ('VISITORS',        'Visitors',          4),
    ('NOTICES',         'Notices',           5),
    ('BOOKINGS',        'Bookings',          6),
    ('HELPDESK',        'Helpdesk',          7),
    ('POLLS',           'Polls',             8),
    ('JOBS',            'Jobs & Referrals',  9),
    ('EVENTS',          'Events',           10),
    ('COMMUNITY_MGMT',  'Community Mgmt',   11),
    ('FINANCE_MGMT',    'Finance Mgmt',     12),
    ('ADMIN_HUB',       'Admin Hub',        13)
) AS m(module_key, module_label, sort_order)
WHERE c.active = TRUE
ON CONFLICT (community_id, module_key) DO NOTHING;
