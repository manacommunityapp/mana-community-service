-- Privacy audit log: tracks sensitive data access and mutations.
-- NEVER store actual PII values here — only resource type/ID references.
-- Retention: 7 years (financial regulation standard).

CREATE TABLE IF NOT EXISTS manacommunity.privacy_audit_log (
    id            BIGSERIAL     PRIMARY KEY,
    actor_id      BIGINT,
    actor_role    VARCHAR(100),
    action        VARCHAR(80)   NOT NULL,
    resource_type VARCHAR(60),
    resource_id   VARCHAR(40),
    community_id  BIGINT,
    reason        VARCHAR(500),
    request_id    VARCHAR(64),
    ip_address    VARCHAR(50),
    timestamp     TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pal_actor     ON manacommunity.privacy_audit_log (actor_id);
CREATE INDEX IF NOT EXISTS idx_pal_resource  ON manacommunity.privacy_audit_log (resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_pal_timestamp ON manacommunity.privacy_audit_log (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_pal_action    ON manacommunity.privacy_audit_log (action);
