-- FILE: db/sql/v1.0.0/14_sports_event_auction_enabled.sql
-- Adds the auction-based intent flag to sports_event (SportsEvent.auctionEnabled).
-- Required for the prod profile (ddl-auto: validate). local=create / dev=update auto-add it.
-- Idempotent: safe to re-run.

ALTER TABLE sports_event
    ADD COLUMN IF NOT EXISTS auction_enabled BOOLEAN DEFAULT FALSE;
