CREATE TABLE IF NOT EXISTS sports_auction_watch (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES app_user(id),
    config_id       BIGINT NOT NULL REFERENCES sports_auction_config(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_auction_watch_user_config UNIQUE (user_id, config_id)
);

CREATE INDEX IF NOT EXISTS idx_auction_watch_user ON sports_auction_watch(user_id);
CREATE INDEX IF NOT EXISTS idx_auction_watch_config ON sports_auction_watch(config_id);
