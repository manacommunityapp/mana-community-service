-- Open-play sessions (matchmaking)
CREATE TABLE IF NOT EXISTS open_play_session (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    sport           VARCHAR(100) NOT NULL,
    court_id        BIGINT NOT NULL,
    session_date    DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    min_players     INTEGER NOT NULL DEFAULT 2,
    max_players     INTEGER NOT NULL,
    skill_level     VARCHAR(50),
    organizer_id    BIGINT NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    notes           VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS open_play_participant (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL REFERENCES open_play_session(id),
    user_id         BIGINT NOT NULL,
    joined_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    confirmed       BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(session_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_ops_comm  ON open_play_session(community_id, sport, session_date, status);
CREATE INDEX IF NOT EXISTS idx_opp_sess  ON open_play_participant(session_id);
CREATE INDEX IF NOT EXISTS idx_opp_user  ON open_play_participant(user_id);
