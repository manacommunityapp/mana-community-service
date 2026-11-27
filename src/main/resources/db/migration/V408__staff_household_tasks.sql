-- V406: Domestic staff household links and resident-assigned tasks
-- Extends the existing domestic_staff (homeservices module) table.

CREATE TABLE IF NOT EXISTS staff_household_link (
    id               BIGSERIAL PRIMARY KEY,
    staff_id         BIGINT NOT NULL,           -- references domestic_staff(id)
    community_id     BIGINT NOT NULL,
    resident_user_id BIGINT NOT NULL,
    flat_number      VARCHAR(50),
    schedule         JSONB,  -- {"mon":["08:00-10:00"],"wed":["08:00-10:00"]}
    since_date       DATE,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(staff_id, resident_user_id)
);

CREATE INDEX idx_staff_household_resident ON staff_household_link(resident_user_id, active);
CREATE INDEX idx_staff_household_staff    ON staff_household_link(staff_id, active);
CREATE INDEX idx_staff_household_community ON staff_household_link(community_id);

CREATE TABLE IF NOT EXISTS staff_task (
    id               BIGSERIAL PRIMARY KEY,
    staff_id         BIGINT NOT NULL,           -- references domestic_staff(id)
    assigned_by      BIGINT NOT NULL,           -- resident user_id
    community_id     BIGINT NOT NULL,
    title            VARCHAR(300) NOT NULL,
    description      TEXT,
    due_date         DATE,
    status           VARCHAR(30) NOT NULL DEFAULT 'OPEN',
                     -- OPEN, IN_PROGRESS, DONE, CANCELLED
    completed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_staff_task_staff       ON staff_task(staff_id, status);
CREATE INDEX idx_staff_task_resident    ON staff_task(assigned_by, status);
CREATE INDEX idx_staff_task_community   ON staff_task(community_id, created_at DESC);
