-- Add optimistic-locking version columns to auction tables.
-- Required for the prod profile (ddl-auto: validate). local=create / dev=update auto-add it.

ALTER TABLE auction_config ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE auction_player ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE auction_team   ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
