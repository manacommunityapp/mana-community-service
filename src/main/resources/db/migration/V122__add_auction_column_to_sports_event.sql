-- FILE: db/migration/V122__add_auction_column_to_sports_event.sql
-- Adds the auction boolean column to sports_event.

ALTER TABLE sports_event
    ADD COLUMN IF NOT EXISTS auction BOOLEAN DEFAULT FALSE;
