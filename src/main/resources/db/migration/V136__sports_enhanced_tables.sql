-- ──────────────────────────────────────────────────────────────────
-- V136: Sports Enhanced Module — scorecard, photos, ratings, stats, badges
-- Run AFTER V135__push_tokens.sql
-- ──────────────────────────────────────────────────────────────────

-- ── 1. sport_match  (if not already created by existing migrations) ───
-- Only run if the table doesn't exist.
CREATE TABLE IF NOT EXISTS sport_match (
    id                             BIGSERIAL     PRIMARY KEY,
    tournament_id                  BIGINT        NOT NULL,
    home_team_id                   BIGINT,
    away_team_id                   BIGINT,
    home_score                     INT           NOT NULL DEFAULT 0,
    away_score                     INT           NOT NULL DEFAULT 0,
    status                         VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    scheduled_at                   TIMESTAMPTZ,
    started_at                     TIMESTAMPTZ,
    ended_at                       TIMESTAMPTZ,
    venue                          VARCHAR(150),
    round                          VARCHAR(50),
    match_number                   INT,
    current_period                 VARCHAR(30),
    elapsed_minutes                INT,
    home_overs                     VARCHAR(10),
    home_wickets                   INT,
    away_overs                     VARCHAR(10),
    away_wickets                   INT,
    home_sets_won                  INT,
    away_sets_won                  INT,
    start_notification_sent_at     TIMESTAMPTZ,
    created_at                     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sm_tournament   ON sport_match (tournament_id);
CREATE INDEX IF NOT EXISTS idx_sm_status       ON sport_match (status);
CREATE INDEX IF NOT EXISTS idx_sm_scheduled    ON sport_match (scheduled_at);

-- ── 2. match_event ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS match_event (
    id           BIGSERIAL    PRIMARY KEY,
    match_id     BIGINT       NOT NULL REFERENCES sport_match(id) ON DELETE CASCADE,
    type         VARCHAR(20)  NOT NULL,
    team_name    VARCHAR(80),
    player_name  VARCHAR(100),
    description  TEXT,
    minute_no    INT,
    over         VARCHAR(10),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_me_match ON match_event (match_id);

-- ── 3. cricket_scorecard ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cricket_scorecard (
    id                BIGSERIAL    PRIMARY KEY,
    match_id          BIGINT       NOT NULL UNIQUE,
    result            VARCHAR(200),
    mom_player_id     BIGINT,
    mom_player_name   VARCHAR(100),
    mom_contribution  VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS batting_entry (
    id                 BIGSERIAL    PRIMARY KEY,
    scorecard_id       BIGINT       NOT NULL REFERENCES cricket_scorecard(id) ON DELETE CASCADE,
    innings_no         INT          NOT NULL,
    batting_team_id    BIGINT,
    batting_team_name  VARCHAR(80),
    total_runs         INT          NOT NULL DEFAULT 0,
    wickets            INT          NOT NULL DEFAULT 0,
    overs              VARCHAR(10),
    extras             INT          NOT NULL DEFAULT 0,
    player_id          BIGINT,
    player_name        VARCHAR(100) NOT NULL,
    runs               INT          NOT NULL DEFAULT 0,
    balls              INT          NOT NULL DEFAULT 0,
    fours              INT          NOT NULL DEFAULT 0,
    sixes              INT          NOT NULL DEFAULT 0,
    strike_rate        NUMERIC(6,2) NOT NULL DEFAULT 0,
    dismissal          VARCHAR(150),
    is_not_out         BOOLEAN      NOT NULL DEFAULT FALSE,
    position           INT          NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS bowling_entry (
    id           BIGSERIAL    PRIMARY KEY,
    scorecard_id BIGINT       NOT NULL REFERENCES cricket_scorecard(id) ON DELETE CASCADE,
    innings_no   INT          NOT NULL,
    player_id    BIGINT,
    player_name  VARCHAR(100) NOT NULL,
    overs        NUMERIC(5,1) NOT NULL DEFAULT 0,
    maidens      INT          NOT NULL DEFAULT 0,
    runs         INT          NOT NULL DEFAULT 0,
    wickets      INT          NOT NULL DEFAULT 0,
    wides        INT          NOT NULL DEFAULT 0,
    no_balls     INT          NOT NULL DEFAULT 0,
    economy      NUMERIC(5,2) NOT NULL DEFAULT 0
);

-- ── 4. match_photo ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS match_photo (
    id               BIGSERIAL    PRIMARY KEY,
    match_id         BIGINT       NOT NULL,
    uploaded_by_id   BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    image_url        VARCHAR(500) NOT NULL,
    caption          VARCHAR(150),
    like_count       INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mp_match ON match_photo (match_id);

CREATE TABLE IF NOT EXISTS match_photo_like (
    id         BIGSERIAL PRIMARY KEY,
    photo_id   BIGINT    NOT NULL REFERENCES match_photo(id) ON DELETE CASCADE,
    user_id    BIGINT    NOT NULL REFERENCES app_user(id)    ON DELETE CASCADE,
    CONSTRAINT uq_photo_like UNIQUE (photo_id, user_id)
);

-- ── 5. match_player_rating ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS match_player_rating (
    id                BIGSERIAL   PRIMARY KEY,
    match_id          BIGINT      NOT NULL,
    rater_id          BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    rated_player_id   BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    stars             INT         NOT NULL CHECK (stars BETWEEN 1 AND 5),
    reaction          VARCHAR(30),
    is_man_of_match   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_match_rating UNIQUE (match_id, rater_id, rated_player_id)
);

CREATE INDEX IF NOT EXISTS idx_mpr_match ON match_player_rating (match_id);

-- ── 6. player_sport_stat ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS player_sport_stat (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    sport           VARCHAR(20)  NOT NULL,
    matches_played  INT          NOT NULL DEFAULT 0,
    wins            INT          NOT NULL DEFAULT 0,
    losses          INT          NOT NULL DEFAULT 0,
    draws           INT          NOT NULL DEFAULT 0,
    tournaments     INT          NOT NULL DEFAULT 0,
    trophies        INT          NOT NULL DEFAULT 0,
    total_runs      INT          NOT NULL DEFAULT 0,
    highest_score   INT          NOT NULL DEFAULT 0,
    total_wickets   INT          NOT NULL DEFAULT 0,
    best_bowling    VARCHAR(20),
    goals           INT          NOT NULL DEFAULT 0,
    assists         INT          NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_player_sport UNIQUE (user_id, sport)
);

CREATE INDEX IF NOT EXISTS idx_pss_user  ON player_sport_stat (user_id);
CREATE INDEX IF NOT EXISTS idx_pss_sport ON player_sport_stat (sport);

-- ── 7. player_badge ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS player_badge (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    badge_id   VARCHAR(50)  NOT NULL,
    earned_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_player_badge UNIQUE (user_id, badge_id)
);

CREATE INDEX IF NOT EXISTS idx_pb_user ON player_badge (user_id);

-- ── 8. kyc_document (for onboarding KYC upload) ──────────────────────
CREATE TABLE IF NOT EXISTS kyc_document (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    id_type      VARCHAR(30)  NOT NULL,
    id_number    VARCHAR(50)  NOT NULL,
    front_url    VARCHAR(500) NOT NULL,
    back_url     VARCHAR(500),
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    submitted_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    reviewed_at  TIMESTAMPTZ,
    reviewed_by  BIGINT       REFERENCES app_user(id) ON DELETE SET NULL
);
