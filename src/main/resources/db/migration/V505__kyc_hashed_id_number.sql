-- ───────────────────────────────────────────────────────────────────────────────
-- KYC: stop storing government ID numbers, store a peppered digest instead.
--
-- The mobile client no longer transmits raw Aadhaar / PAN numbers. It sends
--   idNumberHash  = lowercase hex SHA-256 of the ID number, whitespace and hyphens
--                   stripped and the remainder upper-cased
--   idNumberLast4 = last 1-4 characters, for masked display only
--
-- 1. kyc_document gains id_number_hash + id_number_last4. The column holds
--    HMAC-SHA256(server pepper, client digest), NOT the client digest: a bare
--    SHA-256 of a 12-digit Aadhaar has only 10^12 candidates and is enumerable
--    offline in milliseconds, so a dump of plain digests is a dump of the numbers.
--    The pepper comes from mana.kyc.id-pepper (env MANA_KYC_ID_PEPPER); with no
--    pepper configured the API refuses to store and returns 503 rather than
--    writing a bare digest. See KycIdNumberProtector.
--
--    NOTE: rows written before this change carry an unpeppered value (or a raw
--    number in the legacy id_number column). There is no migration path from a
--    bare digest to a peppered one without the original number, so those
--    identities have to be re-collected from the resident. Nothing is rewritten
--    or deleted here.
--
-- 2. kyc_verification_requests gains id_number_last4. Its id_number_hash already
--    exists from V440 and now holds the peppered value too.
--
-- 3. kyc_document, created in V136, had drifted badly from the KycDocument entity:
--    the entity maps community_id, document_type, front_image_url, back_image_url,
--    rejection_reason and the BaseAuditEntity columns, none of which existed, so
--    every JPA insert into this table would have failed. The missing columns are
--    added, and the legacy NOT NULL columns (id_type, id_number, front_url) are
--    relaxed because the JPA path never writes them. The legacy native-SQL writer
--    in OnboardingControllers always supplies them, so it is unaffected.
--
-- Both tables are created by earlier migrations (V136, V440) and every statement
-- below is idempotent.
-- ───────────────────────────────────────────────────────────────────────────────

-- 1 + 3. Columns the KycDocument entity needs.
ALTER TABLE kyc_document
    ADD COLUMN IF NOT EXISTS id_number_hash   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS id_number_last4  VARCHAR(4),
    ADD COLUMN IF NOT EXISTS community_id     BIGINT,
    ADD COLUMN IF NOT EXISTS document_type    VARCHAR(30),
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS front_image_url  TEXT,
    ADD COLUMN IF NOT EXISTS back_image_url   TEXT,
    ADD COLUMN IF NOT EXISTS created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by       BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by       BIGINT;

-- Legacy columns the JPA path never populates. NOT NULL is dropped rather than the
-- columns themselves, because the native-SQL writer still reads and writes them.
ALTER TABLE kyc_document ALTER COLUMN id_type   DROP NOT NULL;
ALTER TABLE kyc_document ALTER COLUMN id_number DROP NOT NULL;
ALTER TABLE kyc_document ALTER COLUMN front_url DROP NOT NULL;

-- Carry the legacy representation over so existing rows still render in the new API.
UPDATE kyc_document SET document_type   = id_type   WHERE document_type   IS NULL;
UPDATE kyc_document SET front_image_url = front_url WHERE front_image_url IS NULL;
UPDATE kyc_document SET back_image_url  = back_url
 WHERE back_image_url IS NULL AND back_url IS NOT NULL;

-- 2. Display fragment on the verification-request table.
ALTER TABLE kyc_verification_requests
    ADD COLUMN IF NOT EXISTS id_number_last4 VARCHAR(4);

-- Equality lookups on the peppered digest are the reason a server-wide pepper was
-- chosen over a per-row salt, so they get an index.
CREATE INDEX IF NOT EXISTS idx_kyc_document_id_number_hash
    ON kyc_document (id_number_hash);

CREATE INDEX IF NOT EXISTS idx_kyc_verification_id_number_hash
    ON kyc_verification_requests (id_number_hash);

COMMENT ON COLUMN kyc_document.id_number_hash IS
    'HMAC-SHA256(mana.kyc.id-pepper, client SHA-256 of the normalised ID number). Never the raw number, never the bare client digest.';
COMMENT ON COLUMN kyc_document.id_number_last4 IS
    'Last 1-4 characters of the government ID number. Display only (masked UI).';
COMMENT ON COLUMN kyc_document.id_number IS
    'DEPRECATED legacy column: raw/encrypted ID number written by the pre-hash onboarding path. Do not write from new code.';
COMMENT ON COLUMN kyc_verification_requests.id_number_hash IS
    'HMAC-SHA256(mana.kyc.id-pepper, client SHA-256 of the normalised ID number). Rows created before V505 hold an unpeppered digest and cannot be matched.';
COMMENT ON COLUMN kyc_verification_requests.id_number_last4 IS
    'Last 1-4 characters of the government ID number. Display only (masked UI).';
