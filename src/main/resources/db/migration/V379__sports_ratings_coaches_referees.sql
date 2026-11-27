-- Sports: player ratings, coach packages, referee assignment

CREATE TABLE player_rating (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    rated_user_id   BIGINT NOT NULL,
    rated_by        BIGINT NOT NULL,
    sport           VARCHAR(100) NOT NULL,
    session_id      BIGINT,
    rating          INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    sportsmanship   INT CHECK (sportsmanship BETWEEN 1 AND 5),
    skill_level     INT CHECK (skill_level BETWEEN 1 AND 5),
    comment         VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (community_id, rated_user_id, rated_by, sport, session_id)
);
CREATE INDEX idx_pr_community ON player_rating(community_id);
CREATE INDEX idx_pr_rated_user ON player_rating(rated_user_id);

CREATE TABLE coach_package (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    coach_id        BIGINT NOT NULL,
    sport           VARCHAR(100) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    sessions        INT NOT NULL,
    duration_minutes INT NOT NULL,
    price           NUMERIC(12,2) NOT NULL,
    group_size      INT NOT NULL DEFAULT 1,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cp_community ON coach_package(community_id);
CREATE INDEX idx_cp_coach ON coach_package(coach_id);

CREATE TABLE coach_package_enrollment (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    package_id      BIGINT NOT NULL REFERENCES coach_package(id),
    user_id         BIGINT NOT NULL,
    sessions_used   INT NOT NULL DEFAULT 0,
    enrolled_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP WITH TIME ZONE,
    active          BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_cpe_package ON coach_package_enrollment(package_id);
CREATE INDEX idx_cpe_user ON coach_package_enrollment(user_id);

CREATE TABLE referee_assignment (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    referee_id      BIGINT NOT NULL,
    event_id        BIGINT NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    sport           VARCHAR(100) NOT NULL,
    assigned_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    confirmed       BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_at    TIMESTAMP WITH TIME ZONE,
    fee             NUMERIC(12,2),
    notes           VARCHAR(500)
);
CREATE INDEX idx_ra_community ON referee_assignment(community_id);
CREATE INDEX idx_ra_referee ON referee_assignment(referee_id);
CREATE INDEX idx_ra_event ON referee_assignment(event_id);
