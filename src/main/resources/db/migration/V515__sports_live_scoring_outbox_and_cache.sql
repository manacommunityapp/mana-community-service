-- =============================================================================
-- V515__sports_live_scoring_outbox_and_cache.sql
-- Live scoring transactional outbox and event ordering for Mana Sports Engine
-- =============================================================================

CREATE TABLE IF NOT EXISTS sports_match_event_outbox (
    id              BIGSERIAL PRIMARY KEY,
    match_id        BIGINT NOT NULL,
    sequence_no     BIGINT NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    payload         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMP,
    error_message   VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON sports_match_event_outbox(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_match_sequence ON sports_match_event_outbox(match_id, sequence_no);

CREATE TABLE IF NOT EXISTS sports_live_score (
    match_id        BIGINT PRIMARY KEY,
    current_innings INT NOT NULL DEFAULT 1,
    current_sequence BIGINT NOT NULL DEFAULT 0,
    team_a_score    VARCHAR(30),
    team_b_score    VARCHAR(30),
    status          VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    snapshot_json   TEXT,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_score_updated ON sports_live_score(updated_at);
