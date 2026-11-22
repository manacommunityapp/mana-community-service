-- ─────────────────────────────────────────────────────────────────────────────
-- 1. stored_file: give it a real tenant column
--
-- stored_file (V41) has no community column; its only ownership field is
-- uploaded_by, so FileStorageController had to look the uploader up through
-- app_user on every single request, and rows with uploaded_by IS NULL carried no
-- ownership information at all and became SUPER_ADMIN-only — which silently 403s
-- legacy uploads for the people who own them.
--
-- community_id is left NULLABLE on purpose: rows whose uploaded_by is NULL (or
-- whose uploader has since been deleted, or whose uploader has no community) are
-- not derivable, and guessing a tenant for a file is worse than refusing to serve
-- it. Those rows stay NULL and stay SUPER_ADMIN-only, exactly as today.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE manacommunity.stored_file ADD COLUMN IF NOT EXISTS community_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_stored_file_community_id
    ON manacommunity.stored_file (community_id);

-- Back-fill from the uploader. app_user.id is the PK (V41-era schema) and
-- app_user.community_id is the tenant FK (see V39's composite indexes and
-- AppUser.community -> @JoinColumn(name = "community_id")).
UPDATE manacommunity.stored_file sf
   SET community_id = (SELECT au.community_id
                         FROM manacommunity.app_user au
                        WHERE au.id = sf.uploaded_by)
 WHERE sf.uploaded_by IS NOT NULL
   AND sf.community_id IS NULL;

COMMENT ON COLUMN manacommunity.stored_file.community_id IS
    'Owning community (tenant). Back-filled in V506 from app_user.community_id via uploaded_by. '
    'NULL where the uploader is unknown or has no community; such rows are SUPER_ADMIN-only.';

-- How many rows this migration could not attribute. Emitted as a NOTICE so it
-- lands in the Flyway/application log of whatever environment runs the migration;
-- the count cannot be known from the schema alone.
DO $$
DECLARE
    unattributed BIGINT;
    total        BIGINT;
BEGIN
    SELECT count(*) INTO total FROM manacommunity.stored_file;
    SELECT count(*) INTO unattributed
      FROM manacommunity.stored_file
     WHERE community_id IS NULL;
    RAISE NOTICE 'V506: stored_file rows=%; still unattributed (community_id IS NULL, SUPER_ADMIN-only)=%',
                 total, unattributed;
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Widen the health columns that AES-256-GCM ciphertext does not fit into
--
-- V499 declared these columns encrypted but changed no types. Base64(12-byte IV +
-- plaintext + 16-byte GCM tag) is ~4/3 * (len + 28) characters, so encrypting a
-- 10-digit phone number yields ~52 characters — which does not fit
-- physician_phone VARCHAR(20) or emergency_contact.phone VARCHAR(20). Today that
-- is a "value too long for type character varying(20)" error on any write of
-- those fields, and it would abort the backfill job as well. The JPA entities
-- already declare length = 255, so the DDL was simply never brought in line;
-- TEXT removes the ceiling entirely. varchar(n) -> text needs no table rewrite in
-- PostgreSQL.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE manacommunity.health_profile ALTER COLUMN medical_id_number TYPE TEXT;
ALTER TABLE manacommunity.health_profile ALTER COLUMN physician_name    TYPE TEXT;
ALTER TABLE manacommunity.health_profile ALTER COLUMN physician_phone   TYPE TEXT;

ALTER TABLE manacommunity.emergency_contact ALTER COLUMN name  TYPE TEXT;
ALTER TABLE manacommunity.emergency_contact ALTER COLUMN phone TYPE TEXT;
ALTER TABLE manacommunity.emergency_contact ALTER COLUMN email TYPE TEXT;

-- allergies / medical_conditions / emergency_notes are already TEXT (V216).

COMMENT ON COLUMN manacommunity.health_profile.medical_id_number IS
    'AES-256-GCM encrypted at application layer; widened to TEXT in V506 to hold Base64 ciphertext';
COMMENT ON COLUMN manacommunity.health_profile.physician_name IS
    'AES-256-GCM encrypted at application layer; widened to TEXT in V506 to hold Base64 ciphertext';
COMMENT ON COLUMN manacommunity.health_profile.physician_phone IS
    'AES-256-GCM encrypted at application layer; widened to TEXT in V506 to hold Base64 ciphertext';
COMMENT ON COLUMN manacommunity.emergency_contact.name IS
    'AES-256-GCM encrypted at application layer; widened to TEXT in V506 to hold Base64 ciphertext';
COMMENT ON COLUMN manacommunity.emergency_contact.phone IS
    'AES-256-GCM encrypted at application layer; widened to TEXT in V506 to hold Base64 ciphertext';
COMMENT ON COLUMN manacommunity.emergency_contact.email IS
    'AES-256-GCM encrypted at application layer; widened to TEXT in V506 to hold Base64 ciphertext';
