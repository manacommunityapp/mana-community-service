-- ───────────────────────────────────────────────────────────────────────────────
-- V507: marketplace seller KYC — stop storing a plaintext PAN, encrypt the
--       settlement account number, leave the GSTIN alone.
--
-- market_seller_profiles (V168) held gstin, pan_number and bank_account_number as
-- plain VARCHAR(50). That is the same class of problem V505 removed from resident
-- KYC, in a module nobody had looked at. The three fields are not equivalent and
-- are treated differently on purpose:
--
--   PAN                 identifies a person and is never needed in the clear by
--                       this service, so it follows resident KYC exactly: the
--                       client sends SHA-256(normalised PAN) and the server stores
--                       HMAC-SHA256(mana.kyc.id-pepper, that digest) plus the last
--                       4 characters for masked display. See KycIdNumberProtector.
--                       With no pepper configured the API returns 503 rather than
--                       storing a bare digest.
--
--   bank account number has to reach the bank as real digits to pay a seller, so a
--                       digest would destroy the field's purpose. It becomes
--                       AES-256-GCM encrypted at rest through
--                       security/EncryptedStringConverter (the converter already in
--                       use on the health tables), with the last 4 split into their
--                       own plaintext column so no read path decrypts just to mask.
--
--   GSTIN               stays in the clear. It is a published identifier: the GST
--                       department exposes every GSTIN with trade name and address
--                       through public taxpayer search, so an admin checking a
--                       seller against that registry needs the real value, and
--                       protecting it would add friction and no confidentiality.
--                       (Characters 3-12 of a GSTIN are the holder's PAN, so for a
--                       GST-registered seller the PAN is derivable from public data
--                       regardless of what this table does. The PAN protection
--                       above therefore buys real confidentiality mainly for
--                       sellers who are not GST-registered.)
--
-- EXISTING ROWS
--   * pan_number holds plaintext PANs. A peppered HMAC cannot be derived from them
--     in SQL (the pepper lives only in the environment, never in the database), so
--     this migration takes the one safe, useful step: it back-fills
--     pan_number_last4 from the plaintext and then NULLs pan_number, erasing the
--     plaintext while keeping masked display working. Those sellers have no
--     verifiable PAN digest until they re-submit KYC from an updated client.
--   * bank_account_number holds plaintext account numbers. Nothing can encrypt them
--     here either — the AES key is application-side. FieldEncryptionService.decrypt()
--     passes non-ciphertext through unchanged (the same deliberate behaviour the
--     health columns rely on), so those rows keep reading correctly and become
--     ciphertext on their next write. bank_account_last4 is back-filled from them
--     now so masked display works immediately. Encrypting them in place needs a
--     read-modify-write job like backfill/HealthEncryptionBackfillJob; none is added
--     here.
--
-- Every statement is idempotent.
-- ───────────────────────────────────────────────────────────────────────────────

ALTER TABLE market_seller_profiles
    ADD COLUMN IF NOT EXISTS pan_number_hash    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pan_number_last4   VARCHAR(4),
    ADD COLUMN IF NOT EXISTS bank_account_last4 VARCHAR(4);

-- Base64(12-byte IV + plaintext + 16-byte GCM tag) is ~4/3 * (len + 28) characters,
-- so a 16-digit account encrypts to ~60 characters and would not fit VARCHAR(50) —
-- the identical mismatch V506 had to repair on the health columns, where it showed
-- up as "value too long for type character varying(20)" on every write. TEXT removes
-- the ceiling, and varchar(n) -> text needs no table rewrite in PostgreSQL.
ALTER TABLE market_seller_profiles
    ALTER COLUMN bank_account_number TYPE TEXT;

-- Masked fragment for existing plaintext account numbers. The length guard keeps this
-- from slicing the tail off a value that is already ciphertext, which cannot happen on
-- a first run (Flyway runs before the new code serves traffic) but makes a re-run safe.
UPDATE market_seller_profiles
   SET bank_account_last4 = right(bank_account_number, 4)
 WHERE bank_account_last4 IS NULL
   AND bank_account_number IS NOT NULL
   AND length(bank_account_number) BETWEEN 4 AND 25;

-- PAN: keep the display fragment, then erase the plaintext.
UPDATE market_seller_profiles
   SET pan_number_last4 = right(pan_number, 4)
 WHERE pan_number_last4 IS NULL
   AND pan_number IS NOT NULL
   AND length(pan_number) >= 4;

UPDATE market_seller_profiles
   SET pan_number = NULL
 WHERE pan_number IS NOT NULL;

-- The column is kept rather than dropped, matching how V505 retired
-- kyc_document.id_number: a drop is irreversible and the comment is what stops new
-- code writing to it. It is no longer mapped by MarketSellerProfile.
COMMENT ON COLUMN market_seller_profiles.pan_number IS
    'DEPRECATED and emptied in V507: held a plaintext PAN. Never write from new code — use pan_number_hash + pan_number_last4.';
COMMENT ON COLUMN market_seller_profiles.pan_number_hash IS
    'HMAC-SHA256(mana.kyc.id-pepper, client SHA-256 of the normalised PAN). Never the PAN, never the bare client digest.';
COMMENT ON COLUMN market_seller_profiles.pan_number_last4 IS
    'Last 1-4 characters of the PAN. Display only (masked UI).';
COMMENT ON COLUMN market_seller_profiles.bank_account_number IS
    'Settlement account number, AES-256-GCM encrypted at the application layer (security/EncryptedStringConverter). Rows written before V507 hold plaintext and are re-encrypted on their next write.';
COMMENT ON COLUMN market_seller_profiles.bank_account_last4 IS
    'Last 4 digits of the settlement account, in the clear, so masked display and the admin review list never decrypt the full number.';
COMMENT ON COLUMN market_seller_profiles.gstin IS
    'GSTIN, deliberately stored in the clear: a published identifier that an admin must be able to check against public GST taxpayer search. Not a secret.';

-- How many sellers lost a PAN they will have to re-submit, and how many still hold a
-- plaintext account number. Emitted as a NOTICE so it lands in the Flyway log of
-- whichever environment runs this; the counts cannot be known from the schema alone.
DO $$
DECLARE
    pan_only_last4 BIGINT;
    plaintext_bank BIGINT;
BEGIN
    SELECT count(*) INTO pan_only_last4
      FROM market_seller_profiles
     WHERE pan_number_last4 IS NOT NULL AND pan_number_hash IS NULL;
    SELECT count(*) INTO plaintext_bank
      FROM market_seller_profiles
     WHERE bank_account_number IS NOT NULL AND length(bank_account_number) <= 25;
    RAISE NOTICE 'V507: sellers with a masked PAN but no peppered digest (must re-submit KYC)=%; sellers whose bank_account_number is still plaintext (re-encrypted on next write)=%',
                 pan_only_last4, plaintext_bank;
END $$;
