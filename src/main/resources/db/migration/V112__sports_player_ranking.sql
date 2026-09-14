CREATE TABLE IF NOT EXISTS manacommunity.sports_player_ranking (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES manacommunity.app_user(id),
    sport_id    BIGINT       NOT NULL REFERENCES manacommunity.sports_meta(id),
    community_id BIGINT      NOT NULL REFERENCES manacommunity.community(id),
    rank        INTEGER,
    rating      INTEGER,
    source      VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    season      VARCHAR(20)  NOT NULL DEFAULT 'CURRENT',
    notes       TEXT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    CONSTRAINT uq_sports_player_ranking UNIQUE (user_id, sport_id, community_id, season)
);

CREATE INDEX IF NOT EXISTS idx_spr_sport_community ON manacommunity.sports_player_ranking (sport_id, community_id);
CREATE INDEX IF NOT EXISTS idx_spr_user             ON manacommunity.sports_player_ranking (user_id);
CREATE INDEX IF NOT EXISTS idx_spr_rank             ON manacommunity.sports_player_ranking (community_id, sport_id, rank NULLS LAST);

ALTER TABLE manacommunity.sports_event_registration ADD COLUMN IF NOT EXISTS seed INTEGER;
ALTER TABLE manacommunity.sports_auction_player     ADD COLUMN IF NOT EXISTS rating INTEGER;
