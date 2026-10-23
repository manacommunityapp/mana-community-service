-- V94: Community block configuration
-- Stores the per-block layout (floors + flats-per-floor) so the frontend
-- can dynamically populate the Block -> Floor -> Flat Number dropdowns.

CREATE TABLE IF NOT EXISTS manacommunity.community_block_config (
    id               BIGSERIAL PRIMARY KEY,
    community_id     BIGINT       NOT NULL
                         REFERENCES manacommunity.community(id) ON DELETE CASCADE,
    block_name       VARCHAR(10)  NOT NULL,
    total_floors     SMALLINT     NOT NULL DEFAULT 10,
    flats_per_floor  SMALLINT     NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_community_block UNIQUE (community_id, block_name)
);

CREATE INDEX IF NOT EXISTS idx_block_config_community
    ON manacommunity.community_block_config (community_id);
