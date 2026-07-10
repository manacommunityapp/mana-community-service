-- ═══════════════════════════════════════════════════════════════════════════
-- Live Scoring: ball-by-ball event log for real-time match tracking
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS match_ball_event (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id        BIGINT NOT NULL,
    innings_number  INT    NOT NULL DEFAULT 1,
    over_number     INT    NOT NULL DEFAULT 0,
    ball_number     INT    NOT NULL DEFAULT 1,
    delivery_number INT    NOT NULL DEFAULT 1,

    -- Ball outcome
    runs_scored     INT    NOT NULL DEFAULT 0,
    is_boundary     BOOLEAN NOT NULL DEFAULT FALSE,
    is_six          BOOLEAN NOT NULL DEFAULT FALSE,

    -- Extras
    extras_type     VARCHAR(20),
    extras_runs     INT    NOT NULL DEFAULT 0,

    -- Wicket
    is_wicket       BOOLEAN NOT NULL DEFAULT FALSE,
    dismissal_type  VARCHAR(30),
    dismissed_player_id BIGINT,
    fielder_id      BIGINT,

    -- Players involved
    batsman_id      BIGINT NOT NULL,
    non_striker_id  BIGINT,
    bowler_id       BIGINT NOT NULL,

    -- Running totals at the time of this ball
    total_runs      INT    NOT NULL DEFAULT 0,
    total_wickets   INT    NOT NULL DEFAULT 0,
    total_overs     VARCHAR(10) NOT NULL DEFAULT '0.0',

    -- Metadata
    commentary      VARCHAR(500),
    is_undone       BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      BIGINT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ball_match     FOREIGN KEY (match_id)             REFERENCES tournament_match(id) ON DELETE CASCADE,
    CONSTRAINT fk_ball_batsman   FOREIGN KEY (batsman_id)           REFERENCES auction_players(id)  ON DELETE CASCADE,
    CONSTRAINT fk_ball_nstriker  FOREIGN KEY (non_striker_id)       REFERENCES auction_players(id)  ON DELETE SET NULL,
    CONSTRAINT fk_ball_bowler    FOREIGN KEY (bowler_id)            REFERENCES auction_players(id)  ON DELETE CASCADE,
    CONSTRAINT fk_ball_dismissed FOREIGN KEY (dismissed_player_id)  REFERENCES auction_players(id)  ON DELETE SET NULL,
    CONSTRAINT fk_ball_fielder   FOREIGN KEY (fielder_id)           REFERENCES auction_players(id)  ON DELETE SET NULL,
    CONSTRAINT fk_ball_creator   FOREIGN KEY (created_by)           REFERENCES users(id)            ON DELETE SET NULL,

    INDEX idx_ball_match_innings (match_id, innings_number, delivery_number)
);
