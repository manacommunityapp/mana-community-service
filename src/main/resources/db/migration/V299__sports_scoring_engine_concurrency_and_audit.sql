-- V235: Sports Scoring Engine Concurrency, Idempotency, Audit & Officials
-- Adds versioning to sports_tournament_match, command idempotency, corrections audit, and match officials

-- 1. Optimistic locking on matches
ALTER TABLE manacommunity.sports_tournament_match
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. Command correlation on event tables
ALTER TABLE manacommunity.sports_match_event
    ADD COLUMN IF NOT EXISTS command_id UUID;

ALTER TABLE manacommunity.sports_match_ball_event
    ADD COLUMN IF NOT EXISTS command_id UUID;

-- 3. Idempotent command store
CREATE TABLE IF NOT EXISTS manacommunity.sports_scoring_command (
    command_id        UUID PRIMARY KEY,
    match_id          BIGINT NOT NULL,
    sequence_no       BIGINT NOT NULL DEFAULT 1,
    command_type      VARCHAR(50) NOT NULL,
    payload           TEXT,
    response_snapshot TEXT,
    applied_by        BIGINT,
    applied_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scoring_cmd_match FOREIGN KEY (match_id)
        REFERENCES manacommunity.sports_tournament_match(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_scoring_cmd_match_seq
    ON manacommunity.sports_scoring_command (match_id, sequence_no);

-- 4. Immutable scoring correction audit ledger
CREATE TABLE IF NOT EXISTS manacommunity.sports_scoring_correction (
    id                BIGSERIAL PRIMARY KEY,
    match_id          BIGINT NOT NULL,
    original_event_id BIGINT,
    correction_type   VARCHAR(50) NOT NULL,
    reason            VARCHAR(500) NOT NULL,
    corrected_by      BIGINT,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scoring_corr_match FOREIGN KEY (match_id)
        REFERENCES manacommunity.sports_tournament_match(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_scoring_corr_match
    ON manacommunity.sports_scoring_correction (match_id);

-- 5. Match-level referee & scorer official assignments
CREATE TABLE IF NOT EXISTS manacommunity.sports_match_official (
    id          BIGSERIAL PRIMARY KEY,
    match_id    BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    role        VARCHAR(50) NOT NULL DEFAULT 'SCORER',
    assigned_by BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_official_match FOREIGN KEY (match_id)
        REFERENCES manacommunity.sports_tournament_match(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_official_user FOREIGN KEY (user_id)
        REFERENCES manacommunity.app_user(id) ON DELETE CASCADE,
    CONSTRAINT uq_match_official UNIQUE (match_id, user_id, role)
);

CREATE INDEX IF NOT EXISTS idx_match_official_match_user
    ON manacommunity.sports_match_official (match_id, user_id);
