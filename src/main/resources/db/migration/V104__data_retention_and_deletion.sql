-- Data Retention Policies, Data Deletion Requests, and User Privacy Settings

CREATE TABLE IF NOT EXISTS manacommunity.data_retention_policy (
    id                     BIGSERIAL     PRIMARY KEY,
    community_id           BIGINT,
    data_category          VARCHAR(60)   NOT NULL, -- VISITOR_LOGS, INACTIVE_USERS, NOTIFICATIONS, AUDIT_LOGS, etc.
    retention_period_days  INT           NOT NULL, -- e.g. 90, 180, 365, 2555 (7 yrs)
    action_on_expiry       VARCHAR(30)   NOT NULL DEFAULT 'ANONYMIZE', -- DELETE, ANONYMIZE, ARCHIVE
    is_active              BOOLEAN       NOT NULL DEFAULT TRUE,
    description            VARCHAR(255),
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_drp_community ON manacommunity.data_retention_policy (community_id);
CREATE INDEX IF NOT EXISTS idx_drp_category  ON manacommunity.data_retention_policy (data_category);

CREATE TABLE IF NOT EXISTS manacommunity.data_deletion_request (
    id                  BIGSERIAL     PRIMARY KEY,
    user_id             BIGINT        NOT NULL,
    community_id        BIGINT,
    status              VARCHAR(30)   NOT NULL DEFAULT 'PENDING', -- PENDING, VERIFIED, PROCESSING, COMPLETED, REJECTED, CANCELLED
    reason              VARCHAR(500),
    verification_token  VARCHAR(100),
    verification_expiry TIMESTAMP,
    requested_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMP,
    processed_by        BIGINT,
    notes               TEXT
);

CREATE INDEX IF NOT EXISTS idx_ddr_user   ON manacommunity.data_deletion_request (user_id);
CREATE INDEX IF NOT EXISTS idx_ddr_status ON manacommunity.data_deletion_request (status);

CREATE TABLE IF NOT EXISTS manacommunity.user_privacy_settings (
    id                          BIGSERIAL     PRIMARY KEY,
    user_id                     BIGINT        NOT NULL UNIQUE,
    show_phone_to_neighbours    BOOLEAN       NOT NULL DEFAULT FALSE,
    show_email_to_neighbours    BOOLEAN       NOT NULL DEFAULT FALSE,
    show_flat_in_directory      BOOLEAN       NOT NULL DEFAULT TRUE,
    show_family_members         BOOLEAN       NOT NULL DEFAULT FALSE,
    allow_marketplace_contact   BOOLEAN       NOT NULL DEFAULT TRUE,
    allow_event_tagging         BOOLEAN       NOT NULL DEFAULT TRUE,
    activity_visibility         VARCHAR(30)   NOT NULL DEFAULT 'COMMUNITY', -- PRIVATE, COMMUNITY, PUBLIC
    created_at                  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ups_user ON manacommunity.user_privacy_settings (user_id);

-- Insert default retention policies if none exist
INSERT INTO manacommunity.data_retention_policy (community_id, data_category, retention_period_days, action_on_expiry, is_active, description)
VALUES 
    (NULL, 'VISITOR_LOGS',        90,   'ANONYMIZE', TRUE, 'Visitor pass history retention period (90 days)'),
    (NULL, 'NOTIFICATIONS',       60,   'DELETE',    TRUE, 'User in-app notifications retention period (60 days)'),
    (NULL, 'USER_SESSIONS',       30,   'DELETE',    TRUE, 'Expired user login sessions (30 days)'),
    (NULL, 'PRIVACY_AUDIT_LOGS',  2555, 'ARCHIVE',   TRUE, 'Privacy audit logs retention period (7 years)'),
    (NULL, 'MARKETPLACE_ORDERS',  365,  'ANONYMIZE', TRUE, 'Marketplace order delivery address anonymized after 1 year (order record retained)'),
    (NULL, 'EVENT_REGISTRATIONS', 180,  'ANONYMIZE', TRUE, 'Event registration contact details retention period (180 days)')
ON CONFLICT DO NOTHING;
