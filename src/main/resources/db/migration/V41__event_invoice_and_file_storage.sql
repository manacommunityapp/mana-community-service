-- ── File Storage (PostgreSQL fallback) ───────────────────────────────────────
-- Active when app.storage.type=postgres (the default).
-- When S3 is configured, this table is still created but rows are never written;
-- the S3 URL is stored directly on event_invoice.invoice_url instead.
CREATE TABLE IF NOT EXISTS manacommunity.stored_file (
    id            BIGSERIAL    PRIMARY KEY,
    filename      VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    data          BYTEA        NOT NULL,
    uploaded_by   BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- ── Event Invoice ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS manacommunity.event_invoice (
    id             BIGSERIAL     PRIMARY KEY,
    event_id       BIGINT        NOT NULL,
    community_id   BIGINT        NOT NULL,
    created_by     BIGINT,
    invoice_number VARCHAR(100),
    vendor_name    VARCHAR(200)  NOT NULL,
    invoice_date   DATE,
    due_date       DATE,
    amount         NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount   NUMERIC(12,2) NOT NULL DEFAULT 0,
    category       VARCHAR(50)   NOT NULL DEFAULT 'OTHER',
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    invoice_url    VARCHAR(500),
    file_id        BIGINT,
    notes          TEXT,
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_event_invoice_event_id     ON manacommunity.event_invoice(event_id);
CREATE INDEX IF NOT EXISTS idx_event_invoice_community_id ON manacommunity.event_invoice(community_id);
CREATE INDEX IF NOT EXISTS idx_event_invoice_status       ON manacommunity.event_invoice(status);
CREATE INDEX IF NOT EXISTS idx_stored_file_uploaded_by    ON manacommunity.stored_file(uploaded_by);
