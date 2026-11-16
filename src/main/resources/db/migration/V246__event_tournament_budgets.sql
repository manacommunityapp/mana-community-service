-- Phase 6: Module-specific budget management
-- Event budgets and tournament budgets

CREATE TABLE event_budget (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id    BIGINT NOT NULL REFERENCES event_community(id) ON DELETE CASCADE,
    community_id BIGINT NOT NULL,
    total_planned_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    currency    VARCHAR(3) NOT NULL DEFAULT 'INR',
    notes       VARCHAR(1000),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_event_budget_event UNIQUE (event_id)
);

CREATE INDEX idx_event_budget_community ON event_budget(community_id);

CREATE TABLE event_budget_line_item (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    budget_id   BIGINT NOT NULL REFERENCES event_budget(id) ON DELETE CASCADE,
    category    VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    planned_amount NUMERIC(14,2) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_budget_line_budget ON event_budget_line_item(budget_id);

CREATE TABLE tournament_budget (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tournament_id   BIGINT NOT NULL REFERENCES sports_tournament(id) ON DELETE CASCADE,
    community_id    BIGINT NOT NULL,
    total_planned_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
    notes           VARCHAR(1000),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tournament_budget_tournament UNIQUE (tournament_id)
);

CREATE INDEX idx_tournament_budget_community ON tournament_budget(community_id);

CREATE TABLE tournament_budget_line_item (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    budget_id   BIGINT NOT NULL REFERENCES tournament_budget(id) ON DELETE CASCADE,
    category    VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    planned_amount NUMERIC(14,2) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tournament_budget_line_budget ON tournament_budget_line_item(budget_id);
