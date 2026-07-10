-- ═══════════════════════════════════════════════════════════════════════════
-- Match Result & Scorecard tables — layered on top of tournament_match
-- ═══════════════════════════════════════════════════════════════════════════

-- 1) Structured match result (1:1 with tournament_match)
CREATE TABLE IF NOT EXISTS match_result (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id        BIGINT NOT NULL UNIQUE,
    result_type     VARCHAR(30)  NOT NULL DEFAULT 'WIN',
    win_margin      VARCHAR(100),
    toss_winner_team_id  BIGINT,
    toss_decision   VARCHAR(10),
    man_of_match_player_id BIGINT,
    best_batter_player_id  BIGINT,
    best_bowler_player_id  BIGINT,
    match_summary   TEXT,
    umpires         VARCHAR(500),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_match_result_match     FOREIGN KEY (match_id)                REFERENCES tournament_match(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_result_toss      FOREIGN KEY (toss_winner_team_id)     REFERENCES auction_teams(id)    ON DELETE SET NULL,
    CONSTRAINT fk_match_result_mom       FOREIGN KEY (man_of_match_player_id)  REFERENCES auction_players(id)  ON DELETE SET NULL,
    CONSTRAINT fk_match_result_batter    FOREIGN KEY (best_batter_player_id)   REFERENCES auction_players(id)  ON DELETE SET NULL,
    CONSTRAINT fk_match_result_bowler    FOREIGN KEY (best_bowler_player_id)   REFERENCES auction_players(id)  ON DELETE SET NULL
);

-- 2) Innings (2 per match for cricket, or sets for badminton)
CREATE TABLE IF NOT EXISTS match_innings (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_result_id   BIGINT NOT NULL,
    innings_number    INT    NOT NULL DEFAULT 1,
    batting_team_id   BIGINT NOT NULL,
    bowling_team_id   BIGINT NOT NULL,
    total_runs        INT    NOT NULL DEFAULT 0,
    total_wickets     INT    NOT NULL DEFAULT 0,
    total_overs       DECIMAL(5,1) NOT NULL DEFAULT 0.0,
    extras            INT    NOT NULL DEFAULT 0,
    extras_detail     JSON,
    target            INT,
    run_rate          DECIMAL(5,2) DEFAULT 0.00,
    fall_of_wickets   JSON,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_innings_result      FOREIGN KEY (match_result_id) REFERENCES match_result(id) ON DELETE CASCADE,
    CONSTRAINT fk_innings_batting     FOREIGN KEY (batting_team_id) REFERENCES auction_teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_innings_bowling     FOREIGN KEY (bowling_team_id) REFERENCES auction_teams(id) ON DELETE CASCADE
);

-- 3) Batting performance (per batter per innings)
CREATE TABLE IF NOT EXISTS batting_performance (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    innings_id        BIGINT NOT NULL,
    player_id         BIGINT NOT NULL,
    batting_position  INT    NOT NULL DEFAULT 0,
    runs_scored       INT    NOT NULL DEFAULT 0,
    balls_faced       INT    NOT NULL DEFAULT 0,
    fours             INT    NOT NULL DEFAULT 0,
    sixes             INT    NOT NULL DEFAULT 0,
    strike_rate       DECIMAL(6,2) DEFAULT 0.00,
    dismissal_type    VARCHAR(30),
    dismissed_by_id   BIGINT,
    fielder_id        BIGINT,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_batting_innings    FOREIGN KEY (innings_id)      REFERENCES match_innings(id)   ON DELETE CASCADE,
    CONSTRAINT fk_batting_player     FOREIGN KEY (player_id)       REFERENCES auction_players(id) ON DELETE CASCADE,
    CONSTRAINT fk_batting_dismissed  FOREIGN KEY (dismissed_by_id) REFERENCES auction_players(id) ON DELETE SET NULL,
    CONSTRAINT fk_batting_fielder    FOREIGN KEY (fielder_id)      REFERENCES auction_players(id) ON DELETE SET NULL
);

-- 4) Bowling performance (per bowler per innings)
CREATE TABLE IF NOT EXISTS bowling_performance (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    innings_id        BIGINT NOT NULL,
    player_id         BIGINT NOT NULL,
    bowling_order     INT    NOT NULL DEFAULT 0,
    overs_bowled      DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    maidens           INT    NOT NULL DEFAULT 0,
    runs_conceded     INT    NOT NULL DEFAULT 0,
    wickets_taken     INT    NOT NULL DEFAULT 0,
    economy_rate      DECIMAL(5,2) DEFAULT 0.00,
    dot_balls         INT    NOT NULL DEFAULT 0,
    wides             INT    NOT NULL DEFAULT 0,
    no_balls          INT    NOT NULL DEFAULT 0,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bowling_innings    FOREIGN KEY (innings_id) REFERENCES match_innings(id)   ON DELETE CASCADE,
    CONSTRAINT fk_bowling_player     FOREIGN KEY (player_id)  REFERENCES auction_players(id) ON DELETE CASCADE
);

-- 5) Player tournament stats (aggregated, 1 per player per tournament)
CREATE TABLE IF NOT EXISTS player_tournament_stats (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_config_id  BIGINT NOT NULL,
    player_id             BIGINT NOT NULL,
    matches_played        INT    NOT NULL DEFAULT 0,
    innings_batted        INT    NOT NULL DEFAULT 0,
    innings_bowled        INT    NOT NULL DEFAULT 0,
    total_runs            INT    NOT NULL DEFAULT 0,
    highest_score         INT    NOT NULL DEFAULT 0,
    batting_average       DECIMAL(6,2) DEFAULT 0.00,
    batting_strike_rate   DECIMAL(6,2) DEFAULT 0.00,
    fifties               INT    NOT NULL DEFAULT 0,
    hundreds              INT    NOT NULL DEFAULT 0,
    fours                 INT    NOT NULL DEFAULT 0,
    sixes                 INT    NOT NULL DEFAULT 0,
    total_wickets         INT    NOT NULL DEFAULT 0,
    best_bowling          VARCHAR(20),
    bowling_average       DECIMAL(6,2) DEFAULT 0.00,
    economy_rate          DECIMAL(5,2) DEFAULT 0.00,
    catches               INT    NOT NULL DEFAULT 0,
    stumpings             INT    NOT NULL DEFAULT 0,
    run_outs              INT    NOT NULL DEFAULT 0,
    man_of_match_count    INT    NOT NULL DEFAULT 0,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_pts_config  FOREIGN KEY (tournament_config_id) REFERENCES tournament_config(id) ON DELETE CASCADE,
    CONSTRAINT fk_pts_player  FOREIGN KEY (player_id)            REFERENCES auction_players(id)   ON DELETE CASCADE,
    CONSTRAINT uq_pts_config_player UNIQUE (tournament_config_id, player_id)
);

-- Add structured result fields to existing tournament_match
ALTER TABLE tournament_match ADD COLUMN IF NOT EXISTS result_type     VARCHAR(30);
ALTER TABLE tournament_match ADD COLUMN IF NOT EXISTS win_margin      VARCHAR(100);
ALTER TABLE tournament_match ADD COLUMN IF NOT EXISTS toss_winner_team_id BIGINT;
ALTER TABLE tournament_match ADD COLUMN IF NOT EXISTS toss_decision   VARCHAR(10);
ALTER TABLE tournament_match ADD COLUMN IF NOT EXISTS man_of_match_id BIGINT;
ALTER TABLE tournament_match ADD COLUMN IF NOT EXISTS match_summary   TEXT;
ALTER TABLE tournament_match ADD COLUMN IF NOT EXISTS umpires         VARCHAR(500);
