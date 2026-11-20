-- Sports: recurring court slots, open-play matchmaking, waitlists, deposits

CREATE TABLE court_slot_subscription (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    court_id        BIGINT NOT NULL,
    day_of_week     INT NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    frequency       VARCHAR(20) NOT NULL DEFAULT 'WEEKLY',
    start_date      DATE NOT NULL,
    end_date        DATE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_court_sub_community ON court_slot_subscription(community_id);
CREATE INDEX idx_court_sub_user ON court_slot_subscription(user_id);
CREATE INDEX idx_court_sub_court ON court_slot_subscription(court_id);

CREATE TABLE open_play_session (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    sport           VARCHAR(100) NOT NULL,
    court_id        BIGINT NOT NULL,
    session_date    DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    min_players     INT NOT NULL DEFAULT 2,
    max_players     INT NOT NULL,
    skill_level     VARCHAR(50),
    organizer_id    BIGINT NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    notes           VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ops_community ON open_play_session(community_id);
CREATE INDEX idx_ops_date ON open_play_session(session_date);

CREATE TABLE open_play_participant (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL REFERENCES open_play_session(id),
    user_id         BIGINT NOT NULL,
    joined_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    confirmed       BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (session_id, user_id)
);
CREATE INDEX idx_opp_session ON open_play_participant(session_id);

CREATE TABLE court_slot_waitlist (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    booking_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    position        INT NOT NULL,
    notified        BOOLEAN NOT NULL DEFAULT FALSE,
    notified_at     TIMESTAMP WITH TIME ZONE,
    expires_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_waitlist_booking ON court_slot_waitlist(booking_id);
CREATE INDEX idx_waitlist_community ON court_slot_waitlist(community_id);

CREATE TABLE sports_no_show_deposit (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    booking_id      BIGINT NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    held_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    released        BOOLEAN NOT NULL DEFAULT FALSE,
    released_at     TIMESTAMP WITH TIME ZONE,
    forfeited       BOOLEAN NOT NULL DEFAULT FALSE,
    forfeited_at    TIMESTAMP WITH TIME ZONE,
    reason          VARCHAR(500)
);
CREATE INDEX idx_deposit_community ON sports_no_show_deposit(community_id);
CREATE INDEX idx_deposit_user ON sports_no_show_deposit(user_id);
