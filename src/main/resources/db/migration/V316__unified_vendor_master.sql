-- Phase 5: Unified Vendor Master
-- Merges `vendors` (procurement) and `finance_vendors` (finance module) into
-- a single community-scoped `mana_vendor` table.
-- Legacy tables are kept intact; mana_vendor_id columns link back once assigned.

-- ── 1. Create the canonical vendor master ────────────────────────────────────

CREATE TABLE IF NOT EXISTS mana_vendor (
    id                       BIGSERIAL     PRIMARY KEY,

    -- Community scoping (nullable for migrated legacy rows; required for new rows via API)
    community_id             BIGINT        REFERENCES community(id),

    name                     VARCHAR(200)  NOT NULL,
    contact_person           VARCHAR(160),
    email                    VARCHAR(160),
    phone                    VARCHAR(40),
    gstin                    VARCHAR(30),
    pan                      VARCHAR(20),
    address                  VARCHAR(300),
    city                     VARCHAR(80),
    state                    VARCHAR(80),
    pincode                  VARCHAR(20),

    -- From `vendors` table (procurement-specific fields)
    bank_details             VARCHAR(255),
    categories               VARCHAR(200),
    rating                   INT,

    -- Tracks which module originally owned this vendor record
    source_module            VARCHAR(30)   NOT NULL DEFAULT 'FINANCE',

    -- Traceability: original IDs from legacy tables
    legacy_vendor_id         BIGINT,       -- vendors.id
    legacy_finance_vendor_id BIGINT,       -- finance_vendors.id

    status                   VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mana_vendor_community    ON mana_vendor(community_id);
CREATE INDEX IF NOT EXISTS idx_mana_vendor_name         ON mana_vendor(community_id, name);
CREATE INDEX IF NOT EXISTS idx_mana_vendor_legacy_v     ON mana_vendor(legacy_vendor_id);
CREATE INDEX IF NOT EXISTS idx_mana_vendor_legacy_fv    ON mana_vendor(legacy_finance_vendor_id);

-- ── 2. Migrate existing `finance_vendors` rows ───────────────────────────────

INSERT INTO mana_vendor (
    community_id, name, contact_person, email, phone, gstin, pan,
    address, city, state, pincode,
    bank_details, categories, rating,
    source_module, legacy_vendor_id, legacy_finance_vendor_id,
    status, created_at, updated_at
)
SELECT
    NULL,                           -- community_id: requires manual backfill
    name, contact_person, email, phone, gstin, pan,
    address, city, state, pincode,
    NULL, NULL, NULL,               -- bank_details, categories, rating: N/A
    'FINANCE',
    NULL,                           -- legacy_vendor_id
    id,                             -- legacy_finance_vendor_id
    UPPER(status),                  -- normalize 'Active'→'ACTIVE'
    created_at, updated_at
FROM finance_vendors;

-- ── 3. Migrate existing `vendors` rows ───────────────────────────────────────

INSERT INTO mana_vendor (
    community_id, name, contact_person, email, phone, gstin, pan,
    address, city, state, pincode,
    bank_details, categories, rating,
    source_module, legacy_vendor_id, legacy_finance_vendor_id,
    status, created_at, updated_at
)
SELECT
    NULL,                           -- community_id: requires manual backfill
    name, contact_person, email, mobile, gst_number, pan,
    NULL, NULL, NULL, NULL,         -- address fields: N/A
    bank_details, categories, rating,
    'PROCUREMENT',
    id,                             -- legacy_vendor_id
    NULL,                           -- legacy_finance_vendor_id
    CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END,
    created_at, updated_at
FROM vendors;

-- ── 4. Add mana_vendor_id to cfbos_expense ───────────────────────────────────
-- New FK alongside the existing loose vendor_id column.
-- Old vendor_id stays for backward compatibility until FE is fully migrated.

ALTER TABLE cfbos_expense
    ADD COLUMN mana_vendor_id BIGINT REFERENCES mana_vendor(id);

-- ── 5. Add mana_vendor_id to fund_expenditure (if table exists) ──────────────

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'fund_expenditure') THEN
        ALTER TABLE fund_expenditure
            ADD COLUMN IF NOT EXISTS mana_vendor_id BIGINT REFERENCES mana_vendor(id);
    END IF;
END$$;
