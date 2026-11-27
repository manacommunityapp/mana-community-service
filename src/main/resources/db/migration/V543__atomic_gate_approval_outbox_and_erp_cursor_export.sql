-- Atomic Gate Approval CAS + Transactional Outbox + Keyset ERP Receipt Export Schema

CREATE TABLE IF NOT EXISTS atomic_gate_approvals (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    flat_id BIGINT NOT NULL,
    visitor_name VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, DENIED
    version INT NOT NULL DEFAULT 1,
    decided_by BIGINT,
    decided_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS gate_approval_outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING', -- PENDING, PUBLISHED
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS atomic_erp_receipts (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    flat_no VARCHAR(32) NOT NULL,
    amount_paise BIGINT NOT NULL,
    payment_mode VARCHAR(32) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_erp_receipts_keyset_export
    ON atomic_erp_receipts (community_id, paid_at, id);
