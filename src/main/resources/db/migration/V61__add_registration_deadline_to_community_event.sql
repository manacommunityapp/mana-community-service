ALTER TABLE manacommunity.community_event
    ADD COLUMN IF NOT EXISTS registration_deadline DATE;
