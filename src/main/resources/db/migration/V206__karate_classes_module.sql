-- ──────────────────────────────────────────────────────────────────────────────
-- V141: Karate Classes sub-module
--   Tables: karate_belt, karate_program, karate_batch,
--           karate_enrollment, karate_exam, karate_exam_result
--   Seed:   9 standard belt levels (white → black)
-- ──────────────────────────────────────────────────────────────────────────────

-- ── 1. Belt levels ────────────────────────────────────────────────────────────
CREATE TABLE karate_belt (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    sort_order  INT          NOT NULL,
    color_hex   VARCHAR(30)  NOT NULL,
    description VARCHAR(200)
);

INSERT INTO karate_belt (name, sort_order, color_hex, description) VALUES
    ('White',  1, '#FFFFFF', 'Beginner — no prior experience required'),
    ('Yellow', 2, '#FFD700', 'Basic stances and blocks established'),
    ('Orange', 3, '#FF8C00', 'Fundamental kicks and punches introduced'),
    ('Green',  4, '#228B22', 'Intermediate techniques and first kata'),
    ('Blue',   5, '#1E90FF', 'Advanced kata and sparring basics'),
    ('Purple', 6, '#9370DB', 'Complex combinations and self-defence'),
    ('Brown',  7, '#8B4513', 'Advanced sparring and leadership skills'),
    ('Red',    8, '#CC0000', 'Near-black proficiency — advanced kata'),
    ('Black',  9, '#000000', 'Master level — first dan awarded');

-- ── 2. Programs ───────────────────────────────────────────────────────────────
CREATE TABLE karate_program (
    id                BIGSERIAL     PRIMARY KEY,
    name              VARCHAR(150)  NOT NULL,
    description       VARCHAR(500),
    status            VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    min_age           INT,
    max_age           INT,
    starting_belt_id  BIGINT        REFERENCES karate_belt(id) ON DELETE SET NULL,
    target_belt_id    BIGINT        REFERENCES karate_belt(id) ON DELETE SET NULL,
    community_id      BIGINT        REFERENCES community(id)   ON DELETE CASCADE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_karate_program_community ON karate_program (community_id);

-- ── 3. Batches ────────────────────────────────────────────────────────────────
CREATE TABLE karate_batch (
    id              BIGSERIAL    PRIMARY KEY,
    program_id      BIGINT       NOT NULL REFERENCES karate_program(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'UPCOMING',
    start_date      DATE         NOT NULL,
    end_date        DATE,
    class_time      TIME,
    days_of_week    VARCHAR(50),
    max_capacity    INT,
    enrolled_count  INT          NOT NULL DEFAULT 0,
    venue           VARCHAR(200),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_karate_batch_program ON karate_batch (program_id);
CREATE INDEX idx_karate_batch_status  ON karate_batch (status);

-- ── 4. Enrollment ─────────────────────────────────────────────────────────────
CREATE TABLE karate_enrollment (
    id                BIGSERIAL   PRIMARY KEY,
    batch_id          BIGINT      NOT NULL REFERENCES karate_batch(id) ON DELETE CASCADE,
    user_id           BIGINT      NOT NULL REFERENCES app_user(id)     ON DELETE CASCADE,
    current_belt_id   BIGINT      REFERENCES karate_belt(id) ON DELETE SET NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    classes_attended  INT         NOT NULL DEFAULT 0,
    total_classes     INT         NOT NULL DEFAULT 0,
    enrolled_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_karate_enrollment UNIQUE (batch_id, user_id)
);

CREATE INDEX idx_karate_enrollment_batch ON karate_enrollment (batch_id);
CREATE INDEX idx_karate_enrollment_user  ON karate_enrollment (user_id);

-- ── 5. Grading exams ──────────────────────────────────────────────────────────
CREATE TABLE karate_exam (
    id               BIGSERIAL    PRIMARY KEY,
    batch_id         BIGINT       NOT NULL REFERENCES karate_batch(id)  ON DELETE CASCADE,
    target_belt_id   BIGINT       NOT NULL REFERENCES karate_belt(id)   ON DELETE RESTRICT,
    exam_title       VARCHAR(150) NOT NULL,
    exam_date        DATE         NOT NULL,
    notes            VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_karate_exam_batch ON karate_exam (batch_id);

-- ── 6. Exam results ───────────────────────────────────────────────────────────
CREATE TABLE karate_exam_result (
    id               BIGSERIAL    PRIMARY KEY,
    exam_id          BIGINT       NOT NULL REFERENCES karate_exam(id)  ON DELETE CASCADE,
    user_id          BIGINT       NOT NULL REFERENCES app_user(id)     ON DELETE CASCADE,
    result           VARCHAR(20)  NOT NULL,            -- PASS | FAIL | ABSENT
    score            INT          CHECK (score BETWEEN 0 AND 100),
    examiner_notes   VARCHAR(500),
    belt_awarded     BOOLEAN      NOT NULL DEFAULT FALSE,
    awarded_belt_id  BIGINT       REFERENCES karate_belt(id) ON DELETE SET NULL,
    recorded_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_karate_exam_result UNIQUE (exam_id, user_id)
);

CREATE INDEX idx_karate_exam_result_exam ON karate_exam_result (exam_id);
CREATE INDEX idx_karate_exam_result_user ON karate_exam_result (user_id);
