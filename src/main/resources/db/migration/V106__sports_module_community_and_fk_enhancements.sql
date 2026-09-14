-- ── V106: Sports Domain Multi-Tenancy (community_id), Foreign Keys & Indexing ──────────
-- Ensures all sports tables have direct community scoping, FK constraints, and indexes.

-- 1. Add direct community_id and audit columns to sports tables
ALTER TABLE manacommunity.sports_event_registration
    ADD COLUMN IF NOT EXISTS community_id BIGINT REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS reviewed_by_user_id BIGINT REFERENCES manacommunity.app_user(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS partner_confirmation_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS partner_confirmed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS partner_decline_reason VARCHAR(1000);

ALTER TABLE manacommunity.sports_auction_config
    ADD COLUMN IF NOT EXISTS community_id BIGINT REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES manacommunity.app_user(id) ON DELETE SET NULL;

ALTER TABLE manacommunity.sports_auction_team
    ADD COLUMN IF NOT EXISTS community_id BIGINT REFERENCES manacommunity.community(id) ON DELETE CASCADE;

ALTER TABLE manacommunity.sports_auction_player
    ADD COLUMN IF NOT EXISTS community_id BIGINT REFERENCES manacommunity.community(id) ON DELETE CASCADE;

ALTER TABLE manacommunity.sports_tournament_match
    ADD COLUMN IF NOT EXISTS community_id BIGINT REFERENCES manacommunity.community(id) ON DELETE CASCADE;

ALTER TABLE manacommunity.sports_notification_scheduler
    ADD COLUMN IF NOT EXISTS community_id BIGINT REFERENCES manacommunity.community(id) ON DELETE CASCADE;

ALTER TABLE manacommunity.sports_tournament_config
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES manacommunity.app_user(id) ON DELETE SET NULL;

ALTER TABLE manacommunity.sports_tournament
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES manacommunity.app_user(id) ON DELETE SET NULL;

ALTER TABLE manacommunity.sports_event
    ADD COLUMN IF NOT EXISTS mandatory_mixed_doubles BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Backfill community_id from existing parent relations
UPDATE manacommunity.sports_event_registration r
SET community_id = e.community_id
FROM manacommunity.sports_event e
WHERE r.event_id = e.id AND r.community_id IS NULL;

UPDATE manacommunity.sports_auction_config c
SET community_id = e.community_id
FROM manacommunity.sports_event e
WHERE c.event_id = e.id AND c.community_id IS NULL;

UPDATE manacommunity.sports_auction_team t
SET community_id = c.community_id
FROM manacommunity.sports_auction_config c
WHERE t.config_id = c.id AND t.community_id IS NULL;

UPDATE manacommunity.sports_auction_team t
SET community_id = e.community_id
FROM manacommunity.sports_event e
WHERE t.event_id = e.id AND t.community_id IS NULL;

UPDATE manacommunity.sports_auction_player p
SET community_id = c.community_id
FROM manacommunity.sports_auction_config c
WHERE p.config_id = c.id AND p.community_id IS NULL;

UPDATE manacommunity.sports_tournament_match m
SET community_id = c.community_id
FROM manacommunity.sports_tournament_config c
WHERE m.config_id = c.id AND m.community_id IS NULL;

UPDATE manacommunity.sports_notification_scheduler n
SET community_id = e.community_id
FROM manacommunity.sports_event e
WHERE n.event_id = e.id AND n.community_id IS NULL;

UPDATE manacommunity.sports_notification_scheduler n
SET community_id = t.community_id
FROM manacommunity.sports_tournament t
WHERE n.tournament_id = t.id AND n.community_id IS NULL;

-- 3. Add Missing Foreign Key Constraint on sports_auction_team.event_id (if not present)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_sports_auction_team_event'
          AND table_schema = 'manacommunity'
          AND table_name = 'sports_auction_team'
    ) THEN
        ALTER TABLE manacommunity.sports_auction_team
            ADD CONSTRAINT fk_sports_auction_team_event
            FOREIGN KEY (event_id) REFERENCES manacommunity.sports_event(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 4. Multi-Tenant & Query Performance Indexes
CREATE INDEX IF NOT EXISTS idx_sports_event_reg_comm
    ON manacommunity.sports_event_registration(community_id, status);

CREATE INDEX IF NOT EXISTS idx_sports_event_reg_partner
    ON manacommunity.sports_event_registration(partner_user_id, partner_confirmation_status);

CREATE INDEX IF NOT EXISTS idx_sports_auction_cfg_comm
    ON manacommunity.sports_auction_config(community_id, status);

CREATE INDEX IF NOT EXISTS idx_sports_auction_team_comm
    ON manacommunity.sports_auction_team(community_id, config_id);

CREATE INDEX IF NOT EXISTS idx_sports_auction_player_comm
    ON manacommunity.sports_auction_player(community_id, config_id, status);

CREATE INDEX IF NOT EXISTS idx_sports_match_comm_sched
    ON manacommunity.sports_tournament_match(community_id, status, scheduled_at);

CREATE INDEX IF NOT EXISTS idx_sports_notif_comm
    ON manacommunity.sports_notification_scheduler(community_id, enabled, sent);
