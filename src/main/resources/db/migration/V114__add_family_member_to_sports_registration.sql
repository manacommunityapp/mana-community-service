-- ── V114: Add family_member_id to sports_event_registration ──────────
ALTER TABLE manacommunity.sports_event_registration
    ADD COLUMN IF NOT EXISTS family_member_id BIGINT REFERENCES manacommunity.family_members(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_sports_reg_family_member_id
    ON manacommunity.sports_event_registration(family_member_id);
