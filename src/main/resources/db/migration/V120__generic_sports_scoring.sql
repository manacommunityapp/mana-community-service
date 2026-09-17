CREATE TABLE IF NOT EXISTS manacommunity.sports_match_period_score (
    id              BIGSERIAL PRIMARY KEY,
    match_id        BIGINT       NOT NULL,
    period_number   INT          NOT NULL,
    period_label    VARCHAR(255),
    score_team_a    INT          NOT NULL DEFAULT 0,
    score_team_b    INT          NOT NULL DEFAULT 0,
    metadata        TEXT,
    created_at      TIMESTAMP,
    CONSTRAINT fk_period_score_match FOREIGN KEY (match_id) REFERENCES manacommunity.sports_tournament_match (id)
);

CREATE INDEX IF NOT EXISTS idx_period_score_match_id ON manacommunity.sports_match_period_score (match_id);

CREATE TABLE IF NOT EXISTS manacommunity.sports_match_event (
    id                   BIGSERIAL PRIMARY KEY,
    match_id             BIGINT       NOT NULL,
    team_id              BIGINT       NOT NULL,
    player_id            BIGINT,
    event_type           VARCHAR(100) NOT NULL,
    period_number        INT          NOT NULL,
    match_minute         INT,
    points_awarded       INT          NOT NULL DEFAULT 0,
    description          VARCHAR(500),
    secondary_player_id  BIGINT,
    is_undone            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by           BIGINT,
    created_at           TIMESTAMP,
    CONSTRAINT fk_match_event_match FOREIGN KEY (match_id) REFERENCES manacommunity.sports_tournament_match (id),
    CONSTRAINT fk_match_event_team  FOREIGN KEY (team_id)  REFERENCES manacommunity.sports_auction_team (id),
    CONSTRAINT fk_match_event_player FOREIGN KEY (player_id) REFERENCES manacommunity.sports_auction_player (id)
);

CREATE INDEX IF NOT EXISTS idx_match_event_match_id ON manacommunity.sports_match_event (match_id);

CREATE TABLE IF NOT EXISTS manacommunity.sports_player_match_stats (
    id          BIGSERIAL PRIMARY KEY,
    match_id    BIGINT       NOT NULL,
    player_id   BIGINT       NOT NULL,
    team_id     BIGINT       NOT NULL,
    sport_type  VARCHAR(50)  NOT NULL,
    stats_json  TEXT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT fk_player_stats_match  FOREIGN KEY (match_id)  REFERENCES manacommunity.sports_tournament_match (id),
    CONSTRAINT fk_player_stats_player FOREIGN KEY (player_id) REFERENCES manacommunity.sports_auction_player (id),
    CONSTRAINT fk_player_stats_team   FOREIGN KEY (team_id)   REFERENCES manacommunity.sports_auction_team (id)
);

CREATE INDEX IF NOT EXISTS idx_player_stats_match_id  ON manacommunity.sports_player_match_stats (match_id);
CREATE INDEX IF NOT EXISTS idx_player_stats_player_id ON manacommunity.sports_player_match_stats (player_id);

CREATE TABLE IF NOT EXISTS manacommunity.sports_scoring_config (
    id                       BIGSERIAL PRIMARY KEY,
    config_id                BIGINT,
    sport_type               VARCHAR(50) NOT NULL,
    periods_count            INT         NOT NULL DEFAULT 2,
    points_to_win_period     INT,
    must_win_by_two          BOOLEAN     NOT NULL DEFAULT FALSE,
    periods_to_win           INT,
    period_duration_minutes  INT,
    has_overtime             BOOLEAN     NOT NULL DEFAULT FALSE,
    has_penalty_shootout     BOOLEAN     NOT NULL DEFAULT FALSE,
    tiebreak_points_to_win   INT,
    scoring_rules_json       TEXT,
    created_at               TIMESTAMP,
    CONSTRAINT fk_scoring_config_tournament FOREIGN KEY (config_id) REFERENCES manacommunity.sports_tournament_config (id)
);

CREATE INDEX IF NOT EXISTS idx_scoring_config_config_id ON manacommunity.sports_scoring_config (config_id);
