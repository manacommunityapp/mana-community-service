-- V537: Statutory Accounting — Bi-Temporal GST/TDS Rule Engine, Hash-Chained Audit Trail & Tally Sync

CREATE SCHEMA IF NOT EXISTS accounting;

CREATE TABLE IF NOT EXISTS accounting.tax_rule_versions (
    rule_version_id  UUID PRIMARY KEY,
    rule_family      VARCHAR(30) NOT NULL,
    fy_code          VARCHAR(10) NOT NULL,
    version_no       INTEGER NOT NULL,
    effective_from   DATE NOT NULL,
    effective_to     DATE,
    published_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gazette_ref      VARCHAR(200),
    rule_spec        JSONB NOT NULL,
    spec_sha256      CHAR(64) NOT NULL,
    CONSTRAINT uq_tax_rule_family_fy_ver UNIQUE (rule_family, fy_code, version_no)
);

CREATE TABLE IF NOT EXISTS accounting.society_tax_profiles (
    id                       BIGSERIAL PRIMARY KEY,
    society_id               BIGINT NOT NULL,
    fy_code                  VARCHAR(10) NOT NULL,
    gstin                    VARCHAR(15),
    gst_registered           BOOLEAN NOT NULL DEFAULT FALSE,
    aggregate_turnover_paise BIGINT NOT NULL DEFAULT 0,
    books_locked_until       DATE,
    effective_from           DATE NOT NULL,
    effective_to             DATE,
    CONSTRAINT uq_society_tax_profile_eff UNIQUE (society_id, fy_code, effective_from)
);

CREATE TABLE IF NOT EXISTS accounting.vendor_pan_registry (
    pan                  CHAR(10) PRIMARY KEY,
    entity_type_char     CHAR(1) NOT NULL,
    pan_status           VARCHAR(30) NOT NULL DEFAULT 'VALID',
    sec_206ab_specified  BOOLEAN NOT NULL DEFAULT FALSE,
    lower_deduction_rate NUMERIC(5,2),
    lower_deduction_cap  BIGINT,
    verified_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounting.vendor_fy_tds_ledger (
    id                      BIGSERIAL PRIMARY KEY,
    society_id              BIGINT NOT NULL,
    fy_code                 VARCHAR(10) NOT NULL,
    vendor_pan              CHAR(10) NOT NULL,
    tds_section             VARCHAR(20) NOT NULL,
    cumulative_gross_paise  BIGINT NOT NULL DEFAULT 0,
    cumulative_tds_paise    BIGINT NOT NULL DEFAULT 0,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_vendor_fy_tds UNIQUE (society_id, fy_code, vendor_pan, tds_section)
);

CREATE TABLE IF NOT EXISTS accounting.tax_calculation_snapshots (
    snapshot_id       UUID PRIMARY KEY,
    society_id        BIGINT NOT NULL,
    voucher_id        UUID NOT NULL,
    voucher_number    VARCHAR(100) NOT NULL,
    rule_version_id   UUID NOT NULL REFERENCES accounting.tax_rule_versions(rule_version_id),
    rule_family       VARCHAR(30) NOT NULL,
    fy_code           VARCHAR(10) NOT NULL,
    supply_date       DATE NOT NULL,
    base_amount_paise BIGINT NOT NULL,
    computed_tax_paise BIGINT NOT NULL,
    cgst_paise        BIGINT NOT NULL DEFAULT 0,
    sgst_paise        BIGINT NOT NULL DEFAULT 0,
    tds_paise         BIGINT NOT NULL DEFAULT 0,
    applied_rate_bps  INTEGER NOT NULL,
    inputs_json       JSONB NOT NULL,
    snapshot_sha256   CHAR(64) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounting.tax_recalculation_batches (
    batch_id         UUID PRIMARY KEY,
    society_id       BIGINT NOT NULL,
    fy_code          VARCHAR(10) NOT NULL,
    rule_family      VARCHAR(30) NOT NULL,
    target_rule_version_id UUID NOT NULL REFERENCES accounting.tax_rule_versions(rule_version_id),
    from_date        DATE NOT NULL,
    to_date          DATE NOT NULL,
    evaluated_count  INTEGER NOT NULL DEFAULT 0,
    adjusted_count   INTEGER NOT NULL DEFAULT 0,
    net_delta_paise  BIGINT NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounting.tax_adjustment_notes (
    note_id              UUID PRIMARY KEY,
    batch_id             UUID NOT NULL REFERENCES accounting.tax_recalculation_batches(batch_id),
    society_id           BIGINT NOT NULL,
    original_voucher_id  UUID NOT NULL,
    note_type            VARCHAR(20) NOT NULL,
    note_number          VARCHAR(100) NOT NULL,
    delta_paise          BIGINT NOT NULL,
    rule_version_id      UUID NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounting.audit_ledger_entries (
    entry_id             UUID PRIMARY KEY,
    society_id           BIGINT NOT NULL,
    seq_no               BIGINT NOT NULL,
    voucher_id           UUID NOT NULL,
    voucher_number       VARCHAR(100) NOT NULL,
    voucher_type         VARCHAR(30) NOT NULL,
    posting_date         DATE NOT NULL,
    source_system        VARCHAR(20) NOT NULL,
    reverses_entry_id    UUID,
    canonical_lines_json JSONB NOT NULL,
    tax_snapshot_sha256  CHAR(64) NOT NULL,
    prev_entry_hash      CHAR(64) NOT NULL,
    entry_hash           CHAR(64) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_audit_ledger_society_seq UNIQUE (society_id, seq_no)
);

CREATE TABLE IF NOT EXISTS accounting.tally_sync_map (
    id                  BIGSERIAL PRIMARY KEY,
    society_id          BIGINT NOT NULL,
    voucher_id          UUID NOT NULL,
    tally_guid          UUID NOT NULL,
    tally_voucher_number VARCHAR(100) NOT NULL,
    tally_alter_id      BIGINT NOT NULL DEFAULT 0,
    cloud_entry_hash    CHAR(64) NOT NULL,
    synced_payload_hash CHAR(64) NOT NULL,
    owner_source        VARCHAR(20) NOT NULL,
    sync_status         VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    tally_xml           TEXT,
    last_synced_at      TIMESTAMPTZ,
    CONSTRAINT uq_tally_sync_voucher UNIQUE (society_id, voucher_id),
    CONSTRAINT uq_tally_sync_guid UNIQUE (society_id, tally_guid)
);

CREATE TABLE IF NOT EXISTS accounting.tally_sync_conflicts (
    conflict_id          UUID PRIMARY KEY,
    society_id           BIGINT NOT NULL,
    voucher_id           UUID NOT NULL,
    tally_guid           UUID NOT NULL,
    cloud_snapshot_json  JSONB NOT NULL,
    tally_snapshot_json  JSONB NOT NULL,
    resolution_status    VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolved_by_user_id  BIGINT,
    resolved_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tax_rule_lookup ON accounting.tax_rule_versions (rule_family, fy_code, effective_from, effective_to);
CREATE INDEX IF NOT EXISTS idx_tax_snap_society_date ON accounting.tax_calculation_snapshots (society_id, fy_code, supply_date);
CREATE INDEX IF NOT EXISTS idx_audit_ledger_society_date ON accounting.audit_ledger_entries (society_id, posting_date, seq_no);
CREATE INDEX IF NOT EXISTS idx_tally_sync_status ON accounting.tally_sync_map (society_id, sync_status);
CREATE INDEX IF NOT EXISTS idx_tally_conflicts_open ON accounting.tally_sync_conflicts (society_id, resolution_status);

CREATE OR REPLACE FUNCTION accounting.prevent_audit_ledger_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'accounting.audit_ledger_entries is append-only; post a reversal entry instead'
        USING ERRCODE = '42501';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_audit_ledger_mutation ON accounting.audit_ledger_entries;
CREATE TRIGGER trg_prevent_audit_ledger_mutation
    BEFORE UPDATE OR DELETE ON accounting.audit_ledger_entries
    FOR EACH ROW EXECUTE FUNCTION accounting.prevent_audit_ledger_mutation();
