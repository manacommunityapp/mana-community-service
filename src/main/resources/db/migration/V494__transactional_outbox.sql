CREATE TABLE IF NOT EXISTS outbox_event (
    id              BIGSERIAL PRIMARY KEY,
    aggregate_type  VARCHAR(100)  NOT NULL,
    aggregate_id    VARCHAR(255)  NOT NULL,
    event_type      VARCHAR(100)  NOT NULL,
    payload         JSONB         NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retry_count     INT           NOT NULL DEFAULT 0,
    max_retries     INT           NOT NULL DEFAULT 5,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    next_retry_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    processed_at    TIMESTAMPTZ,
    failed_at       TIMESTAMPTZ,
    error_message   TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending
    ON outbox_event (status, next_retry_at)
    WHERE status IN ('PENDING', 'RETRY');

COMMENT ON COLUMN outbox_event.status IS 'PENDING | PROCESSING | PROCESSED | FAILED | RETRY';
COMMENT ON COLUMN outbox_event.aggregate_id IS 'ID of the entity that generated this event (community ID, user ID, etc)';
