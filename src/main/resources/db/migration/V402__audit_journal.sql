CREATE TYPE audit_category AS ENUM (
    'BUSINESS', 'SECURITY', 'PRIVACY', 'FINANCIAL', 'OPERATIONAL'
);

CREATE TABLE audit_journal (
    id              BIGSERIAL       PRIMARY KEY,
    audit_id        UUID            NOT NULL DEFAULT gen_random_uuid(),
    category        audit_category  NOT NULL,
    community_id    BIGINT,
    actor_id        BIGINT,
    actor_type      VARCHAR(50),
    action          VARCHAR(200)    NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       VARCHAR(255),
    details         JSONB,
    correlation_id  VARCHAR(100),
    ip_address      INET,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Append-only: prevent UPDATE and DELETE at the database level
CREATE RULE no_update_audit_journal AS ON UPDATE TO audit_journal DO INSTEAD NOTHING;
CREATE RULE no_delete_audit_journal AS ON DELETE TO audit_journal DO INSTEAD NOTHING;

CREATE INDEX idx_audit_community_time ON audit_journal(community_id, created_at DESC);
CREATE INDEX idx_audit_entity         ON audit_journal(entity_type, entity_id);
CREATE INDEX idx_audit_category_time  ON audit_journal(category, created_at DESC);
CREATE INDEX idx_audit_actor          ON audit_journal(actor_id, created_at DESC);
