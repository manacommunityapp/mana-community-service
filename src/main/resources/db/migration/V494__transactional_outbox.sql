-- V494: Transactional outbox table alignment (infrastructure created in V400)
CREATE TABLE IF NOT EXISTS outbox_event (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100)  NOT NULL,
    aggregate_id    VARCHAR(255)  NOT NULL,
    event_type      VARCHAR(200)  NOT NULL,
    payload         JSONB         NOT NULL,
    community_id    BIGINT,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ,
    retry_count     INT           NOT NULL DEFAULT 0,
    last_error      TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending_v494
    ON outbox_event (created_at)
    WHERE status = 'PENDING';
