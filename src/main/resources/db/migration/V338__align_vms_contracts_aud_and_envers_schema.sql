-- ============================================================================
-- V338: Align Hibernate Envers audit schema
-- Adds termination_reason to vms_contracts_aud and ensures app_user_aud
-- and consent_records_aud schema alignment.
-- Idempotent and backwards-compatible.
-- ============================================================================

-- 1. vms_contracts: ensure termination_reason exists
ALTER TABLE IF EXISTS manacommunity.vms_contracts
    ADD COLUMN IF NOT EXISTS termination_reason VARCHAR(1000);

-- 2. vms_contracts_aud: add missing termination_reason column
ALTER TABLE IF EXISTS manacommunity.vms_contracts_aud
    ADD COLUMN IF NOT EXISTS termination_reason VARCHAR(1000);

-- 3. app_user_aud: ensure organization_id exists
ALTER TABLE IF EXISTS manacommunity.app_user_aud
    ADD COLUMN IF NOT EXISTS organization_id BIGINT;

-- 4. consent_records_aud: ensure table exists for DPDP ConsentRecord @Audited entity
CREATE TABLE IF NOT EXISTS manacommunity.consent_records_aud (
    id                   BIGINT    NOT NULL,
    rev                  INT       NOT NULL REFERENCES manacommunity.revinfo(rev),
    revtype              SMALLINT  NOT NULL,
    user_id              BIGINT,
    consent_purpose      VARCHAR(50),
    granted              BOOLEAN,
    source_ip            VARCHAR(50),
    collection_point     VARCHAR(30),
    notice_version       VARCHAR(20),
    created_at           TIMESTAMP,
    PRIMARY KEY (id, rev)
);
CREATE INDEX IF NOT EXISTS idx_consent_records_aud_rev ON manacommunity.consent_records_aud (rev);
