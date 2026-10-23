-- CricHeroes profile cache table: stores linked CricHeroes player data per auction player
CREATE TABLE IF NOT EXISTS sports_cricheroes_profile (
    id              BIGSERIAL PRIMARY KEY,
    player_id       BIGINT NOT NULL REFERENCES sports_auction_player(id) ON DELETE CASCADE,
    community_id    BIGINT NOT NULL REFERENCES community(id),
    cricheroes_id   VARCHAR(100) NOT NULL,
    share_url       VARCHAR(500) NOT NULL,
    resolved_url    VARCHAR(500),
    format_scope    VARCHAR(20) NOT NULL DEFAULT 'OVERALL',
    bio_json        JSONB NOT NULL DEFAULT '{}',
    batting_json    JSONB NOT NULL DEFAULT '{}',
    bowling_json    JSONB NOT NULL DEFAULT '{}',
    fielding_json   JSONB NOT NULL DEFAULT '{}',
    recent_form_json JSONB NOT NULL DEFAULT '[]',
    mvp_points      INTEGER,
    rating_overall  DOUBLE PRECISION,
    rating_tier     VARCHAR(20),
    rating_badges   VARCHAR(500),
    suggested_base_price INTEGER,
    verified_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    synced_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_sports_cricheroes_player UNIQUE (player_id),
    CONSTRAINT uq_sports_cricheroes_id_community UNIQUE (cricheroes_id, community_id)
);

CREATE INDEX IF NOT EXISTS idx_sports_cricheroes_profile_player ON sports_cricheroes_profile(player_id);
CREATE INDEX IF NOT EXISTS idx_sports_cricheroes_profile_config ON sports_cricheroes_profile(community_id);
CREATE INDEX IF NOT EXISTS idx_sports_cricheroes_profile_ch_id ON sports_cricheroes_profile(cricheroes_id);

-- Add CricHeroes link columns to existing auction player table
ALTER TABLE sports_auction_player ADD COLUMN IF NOT EXISTS cricheroes_id VARCHAR(100);
ALTER TABLE sports_auction_player ADD COLUMN IF NOT EXISTS cricheroes_url VARCHAR(500);
