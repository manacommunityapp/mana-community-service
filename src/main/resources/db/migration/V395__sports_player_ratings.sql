-- Per-game player ratings
CREATE TABLE player_rating (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    rated_user_id   BIGINT NOT NULL,
    rated_by        BIGINT NOT NULL,
    sport           VARCHAR(100) NOT NULL,
    session_id      BIGINT,
    rating          INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    sportsmanship   INTEGER CHECK (sportsmanship BETWEEN 1 AND 5),
    skill_level     INTEGER,
    comment         VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(rated_by, session_id, rated_user_id)
);

-- Aggregated per-player per-sport summary
CREATE TABLE player_rating_summary (
    id                  BIGSERIAL PRIMARY KEY,
    community_id        BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    sport               VARCHAR(100) NOT NULL,
    games_played        INTEGER NOT NULL DEFAULT 0,
    avg_rating          NUMERIC(3,2) NOT NULL DEFAULT 0,
    avg_sportsmanship   NUMERIC(3,2) NOT NULL DEFAULT 0,
    total_ratings       INTEGER NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, sport)
);

CREATE INDEX idx_pr_rated   ON player_rating(rated_user_id, sport);
CREATE INDEX idx_pr_rater   ON player_rating(rated_by);
CREATE INDEX idx_prs_user   ON player_rating_summary(community_id, sport, avg_rating DESC);
