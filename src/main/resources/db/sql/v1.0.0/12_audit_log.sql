-- FILE: db/sql/v1.0.0/12_audit_log.sql
-- Durable audit trail (AuditLog entity / AuditService).
-- Required for the prod profile (ddl-auto: validate). local=create / dev=update auto-create it.
-- No FK on user_id on purpose: audit rows must survive user deletion for retention/compliance.

CREATE TABLE IF NOT EXISTS audit_log (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT,
    action         VARCHAR(60)  NOT NULL,
    module         VARCHAR(40)  NOT NULL,
    entity_name    VARCHAR(60),
    entity_id      VARCHAR(64),
    old_value      TEXT,
    new_value      TEXT,
    ip_address     VARCHAR(64),
    correlation_id VARCHAR(64),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_user   ON audit_log (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_module ON audit_log (module, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log (action, created_at);
