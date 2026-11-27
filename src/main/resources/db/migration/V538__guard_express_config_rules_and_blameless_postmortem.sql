-- Guard Express Rainy-Gate Flow, Declarative Tenant Accounting Rules & Blameless Post-Mortem Guardrails

CREATE TABLE IF NOT EXISTS guard_express_checkins (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    gate_id VARCHAR(64) NOT NULL,
    guard_id BIGINT NOT NULL,
    client_seq_id BIGINT NOT NULL,
    entry_mode VARCHAR(32) NOT NULL, -- COURIER_ICON, OTP_6_DIGIT, QR_PASS, GUARD_OVERRIDE_PHOTO
    brand_code VARCHAR(32),
    flat_no VARCHAR(32) NOT NULL,
    photo_url VARCHAR(512),
    audio_cue VARCHAR(32) NOT NULL,
    requires_mc_review BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_guard_express_gate_seq UNIQUE (gate_id, client_seq_id)
);

CREATE INDEX IF NOT EXISTS idx_guard_express_mc_review
    ON guard_express_checkins (community_id, requires_mc_review, created_at DESC);

CREATE TABLE IF NOT EXISTS tenant_accounting_rules (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL,
    rule_key VARCHAR(64) NOT NULL,
    rule_type VARCHAR(32) NOT NULL, -- PENALTY_WAIVER, GST_OVERRIDE, SINKING_FUND_SURCHARGE, LATE_FEE_CAP
    rate_bps INT NOT NULL DEFAULT 0,
    flat_amount_paise BIGINT NOT NULL DEFAULT 0,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE NOT NULL,
    dry_run_only BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    audit_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tenant_accounting_rule UNIQUE (community_id, rule_key)
);

CREATE TABLE IF NOT EXISTS incident_postmortems (
    id BIGSERIAL PRIMARY KEY,
    incident_code VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    severity VARCHAR(16) NOT NULL, -- SEV1, SEV2, SEV3
    blast_radius_pct DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    root_cause_system VARCHAR(128) NOT NULL,
    mitigation_summary TEXT,
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN', -- OPEN, MITIGATED, CLOSED
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS postmortem_guardrails (
    id BIGSERIAL PRIMARY KEY,
    postmortem_id BIGINT NOT NULL REFERENCES incident_postmortems(id) ON DELETE CASCADE,
    guardrail_type VARCHAR(32) NOT NULL, -- BULKHEAD_POOL, RUSH_HOUR_FREEZE, AUTO_ROLLBACK, CHAOS_GAMEDAY
    description VARCHAR(512) NOT NULL,
    owner_team VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING', -- PENDING, IMPLEMENTED, VERIFIED
    due_at TIMESTAMP WITH TIME ZONE,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_postmortem_guardrails_pm
    ON postmortem_guardrails (postmortem_id, status);
