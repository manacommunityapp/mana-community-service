-- community table may be missing updated_at if V109 was not applied in this environment
ALTER TABLE manacommunity.community
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
