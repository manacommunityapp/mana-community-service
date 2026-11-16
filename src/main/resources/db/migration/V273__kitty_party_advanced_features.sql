-- ============================================================================
-- V273: Mana Kitty Party Advanced Features: Audit History & Disputes
-- Schema: manacommunity
-- ============================================================================

-- 1. Immutable Financial Audit History (Chained SHA-256 for Non-Custodial Integrity)
CREATE TABLE IF NOT EXISTS manacommunity.kitty_financial_audit (
    id                  BIGSERIAL PRIMARY KEY,
    cycle_id            BIGINT NOT NULL REFERENCES manacommunity.kitty_cycle(id) ON DELETE CASCADE,
    circle_id           BIGINT NOT NULL REFERENCES manacommunity.kitty_circle(id) ON DELETE CASCADE,
    event_type          VARCHAR(50) NOT NULL,
    actor_id            BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    description         TEXT NOT NULL,
    metadata_json       TEXT,
    prev_hash           VARCHAR(64),
    curr_hash           VARCHAR(64) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kitty_audit_cycle ON manacommunity.kitty_financial_audit(cycle_id, created_at);
CREATE INDEX IF NOT EXISTS idx_kitty_audit_circle ON manacommunity.kitty_financial_audit(circle_id);

-- 2. Dispute & Complaint Management
CREATE TABLE IF NOT EXISTS manacommunity.kitty_dispute (
    id                  BIGSERIAL PRIMARY KEY,
    circle_id           BIGINT NOT NULL REFERENCES manacommunity.kitty_circle(id) ON DELETE CASCADE,
    cycle_id            BIGINT REFERENCES manacommunity.kitty_cycle(id) ON DELETE CASCADE,
    raised_by           BIGINT NOT NULL REFERENCES manacommunity.app_user(id),
    dispute_type        VARCHAR(40) NOT NULL
                        CHECK (dispute_type IN ('PAYMENT_UNRECOGNIZED', 'PAYOUT_DELAY', 'AMOUNT_MISMATCH', 'RULE_VIOLATION', 'OTHER')),
    title               VARCHAR(200) NOT NULL,
    description         TEXT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    resolution_notes    TEXT,
    resolved_by         BIGINT REFERENCES manacommunity.app_user(id),
    resolved_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kitty_dispute_circle ON manacommunity.kitty_dispute(circle_id, status);
CREATE INDEX IF NOT EXISTS idx_kitty_dispute_cycle ON manacommunity.kitty_dispute(cycle_id);
