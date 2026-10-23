-- V107: Hibernate Envers — shared revision info table
-- One row per JPA transaction flush that touches any @Audited entity.
-- user_id is nullable: NULL means a background thread / scheduler write.

CREATE TABLE IF NOT EXISTS manacommunity.revinfo (
    rev       SERIAL   PRIMARY KEY,
    revtstmp  BIGINT   NOT NULL,
    user_id   BIGINT   NULL
);

CREATE INDEX IF NOT EXISTS idx_revinfo_user
    ON manacommunity.revinfo (user_id)
    WHERE user_id IS NOT NULL;
