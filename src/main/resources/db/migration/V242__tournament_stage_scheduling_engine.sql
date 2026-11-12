-- V242: Tournament Stage-Based Scheduling & Publication Engine
-- Adds tournament stages, schedule versioning, match postponements, and reserve days.

-- ─── Tournament Stages ──────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS sports_tournament_stage (
    id              BIGSERIAL PRIMARY KEY,
    config_id       BIGINT       NOT NULL REFERENCES sports_tournament_config(id) ON DELETE CASCADE,
    stage_name      VARCHAR(100) NOT NULL,
    stage_type      VARCHAR(30)  NOT NULL DEFAULT 'GROUP_STAGE',
    stage_order     INT          NOT NULL DEFAULT 1,
    stage_status    VARCHAR(30)  NOT NULL DEFAULT 'NOT_GENERATED',
    scheduling_mode VARCHAR(30)  NOT NULL DEFAULT 'HYBRID',
    publication_status VARCHAR(30) NOT NULL DEFAULT 'UNPUBLISHED',
    published_version_id BIGINT,
    qualification_rule_id BIGINT,
    start_date      DATE,
    end_date        DATE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_stage_config_order UNIQUE (config_id, stage_order)
);

CREATE INDEX IF NOT EXISTS idx_stage_config_id ON sports_tournament_stage(config_id);

-- ─── Schedule Versions ──────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS sports_schedule_version (
    id                  BIGSERIAL PRIMARY KEY,
    config_id           BIGINT       NOT NULL REFERENCES sports_tournament_config(id) ON DELETE CASCADE,
    stage_id            BIGINT       REFERENCES sports_tournament_stage(id) ON DELETE SET NULL,
    version_number      INT          NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    change_reason       TEXT,
    changed_match_ids_json TEXT,
    changed_fields_json TEXT,
    generated_by        VARCHAR(200),
    approved_by         VARCHAR(200),
    published_by        VARCHAR(200),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    approved_at         TIMESTAMP,
    published_at        TIMESTAMP,

    CONSTRAINT uq_version_config_stage_num UNIQUE (config_id, stage_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_schedule_version_config ON sports_schedule_version(config_id);
CREATE INDEX IF NOT EXISTS idx_schedule_version_stage ON sports_schedule_version(stage_id);

-- ─── Match Postponements ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS sports_match_postponement (
    id                  BIGSERIAL PRIMARY KEY,
    match_id            BIGINT       NOT NULL REFERENCES sports_tournament_match(id) ON DELETE CASCADE,
    original_start_at   TIMESTAMP    NOT NULL,
    proposed_start_at   TIMESTAMP,
    new_start_at        TIMESTAMP,
    reason_code         VARCHAR(50)  NOT NULL,
    reason_description  TEXT,
    scope               VARCHAR(30)  NOT NULL DEFAULT 'SINGLE_MATCH',
    requested_by        VARCHAR(200),
    approved_by         VARCHAR(200),
    approval_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reserve_day_id      BIGINT,
    affected_match_ids_json TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_postponement_match ON sports_match_postponement(match_id);

-- ─── Reserve Days ───────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS sports_tournament_reserve_day (
    id              BIGSERIAL PRIMARY KEY,
    config_id       BIGINT       NOT NULL REFERENCES sports_tournament_config(id) ON DELETE CASCADE,
    stage_id        BIGINT       REFERENCES sports_tournament_stage(id) ON DELETE SET NULL,
    reserve_date    DATE         NOT NULL,
    venue_id        BIGINT       REFERENCES sports_venue(id) ON DELETE SET NULL,
    court_id        BIGINT       REFERENCES sports_court(id) ON DELETE SET NULL,
    start_time      TIME         NOT NULL DEFAULT '09:00',
    end_time        TIME         NOT NULL DEFAULT '18:00',
    reserve_type    VARCHAR(30)  NOT NULL DEFAULT 'TOURNAMENT_WIDE',
    status          VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    activated_at    TIMESTAMP,
    activated_by    VARCHAR(200),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reserve_day_config ON sports_tournament_reserve_day(config_id);
CREATE INDEX IF NOT EXISTS idx_reserve_day_date ON sports_tournament_reserve_day(reserve_date);

-- ─── Stage Publication Records ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS sports_stage_publication (
    id                  BIGSERIAL PRIMARY KEY,
    stage_id            BIGINT       NOT NULL REFERENCES sports_tournament_stage(id) ON DELETE CASCADE,
    version_id          BIGINT       NOT NULL REFERENCES sports_schedule_version(id) ON DELETE CASCADE,
    published_by        VARCHAR(200) NOT NULL,
    published_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    notification_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    recipient_count     INT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_stage_publication_stage ON sports_stage_publication(stage_id);

-- ─── Qualification Rules ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS sports_qualification_rule (
    id                          BIGSERIAL PRIMARY KEY,
    stage_id                    BIGINT       NOT NULL REFERENCES sports_tournament_stage(id) ON DELETE CASCADE,
    qualify_count               INT          NOT NULL DEFAULT 2,
    tie_breakers_json           TEXT,
    auto_advance                BOOLEAN      NOT NULL DEFAULT FALSE,
    requires_admin_confirmation BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_qualification_stage UNIQUE (stage_id)
);

-- ─── Add stage_id FK to tournament matches ──────────────────────────────────

ALTER TABLE sports_tournament_match
    ADD COLUMN IF NOT EXISTS stage_id BIGINT REFERENCES sports_tournament_stage(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_match_stage ON sports_tournament_match(stage_id);

-- ─── Add schedule_version_id to tournament matches ──────────────────────────

ALTER TABLE sports_tournament_match
    ADD COLUMN IF NOT EXISTS schedule_version_id BIGINT REFERENCES sports_schedule_version(id) ON DELETE SET NULL;

-- ─── Add is_reserve_day_match flag ──────────────────────────────────────────

ALTER TABLE sports_tournament_match
    ADD COLUMN IF NOT EXISTS is_reserve_day_match BOOLEAN NOT NULL DEFAULT FALSE;

-- ─── Add postponed_from_match_id for tracking ──────────────────────────────

ALTER TABLE sports_tournament_match
    ADD COLUMN IF NOT EXISTS postponed_from_match_id BIGINT REFERENCES sports_tournament_match(id) ON DELETE SET NULL;
