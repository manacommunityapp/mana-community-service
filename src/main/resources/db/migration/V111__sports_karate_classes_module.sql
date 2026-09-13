-- V111: Sports Karate Classes Module
-- Creates all tables for karate program/batch/enrollment/class/attendance/grading.
-- All tables are community-scoped from day one.

SET search_path TO manacommunity;

-- Belt grade levels (community-configurable, ordered by rank)
CREATE TABLE IF NOT EXISTS sports_karate_belt (
    id                    BIGSERIAL PRIMARY KEY,
    community_id          BIGINT REFERENCES community(id) ON DELETE CASCADE,
    sport_id              BIGINT REFERENCES sports_meta(id) ON DELETE RESTRICT,
    name                  VARCHAR(80)  NOT NULL,
    color_hex             VARCHAR(7),
    rank                  INTEGER      NOT NULL,
    min_classes_required  INTEGER      NOT NULL DEFAULT 0,
    min_months_required   INTEGER      NOT NULL DEFAULT 0,
    description           VARCHAR(500),
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by            BIGINT,
    updated_by            BIGINT,
    UNIQUE (community_id, rank)
);

-- Training programs (e.g. "Kids Karate 2026")
CREATE TABLE IF NOT EXISTS sports_karate_program (
    id                   BIGSERIAL PRIMARY KEY,
    community_id         BIGINT      NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    sport_id             BIGINT      NOT NULL REFERENCES sports_meta(id) ON DELETE RESTRICT,
    instructor_user_id   BIGINT      REFERENCES app_user(id) ON DELETE SET NULL,
    name                 VARCHAR(100) NOT NULL,
    level                VARCHAR(30)  NOT NULL DEFAULT 'BEGINNER',
    min_age              INTEGER,
    max_age              INTEGER,
    monthly_fee          DECIMAL(10,2),
    start_date           DATE,
    end_date             DATE,
    max_students         INTEGER,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    description          VARCHAR(2000),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by           BIGINT,
    updated_by           BIGINT
);

-- Scheduled batches within a program (e.g. "Morning Batch — Mon/Wed/Fri")
CREATE TABLE IF NOT EXISTS sports_karate_batch (
    id                       BIGSERIAL PRIMARY KEY,
    program_id               BIGINT      NOT NULL REFERENCES sports_karate_program(id) ON DELETE CASCADE,
    community_id             BIGINT      NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    venue_id                 BIGINT      REFERENCES venue(id) ON DELETE SET NULL,
    court_id                 BIGINT      REFERENCES sports_court(id) ON DELETE SET NULL,
    batch_name               VARCHAR(80)  NOT NULL,
    days_of_week             VARCHAR(50),
    start_time               TIME,
    end_time                 TIME,
    max_students             INTEGER,
    status                   VARCHAR(30)  NOT NULL DEFAULT 'UPCOMING',
    attendance_threshold     INTEGER      NOT NULL DEFAULT 75,
    auto_generate_classes    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by               BIGINT,
    updated_by               BIGINT
);

-- Student enrollment in a batch
CREATE TABLE IF NOT EXISTS sports_karate_enrollment (
    id                       BIGSERIAL PRIMARY KEY,
    batch_id                 BIGINT      NOT NULL REFERENCES sports_karate_batch(id) ON DELETE CASCADE,
    community_id             BIGINT      NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    student_user_id          BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    current_belt_id          BIGINT      REFERENCES sports_karate_belt(id) ON DELETE SET NULL,
    enrolled_by_user_id      BIGINT      REFERENCES app_user(id) ON DELETE SET NULL,
    status                   VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    enrolled_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    attendance_percentage    DECIMAL(5,2) NOT NULL DEFAULT 0,
    total_classes_attended   INTEGER      NOT NULL DEFAULT 0,
    grading_eligible         BOOLEAN      NOT NULL DEFAULT FALSE,
    last_low_att_alert_at    TIMESTAMP,
    notes                    VARCHAR(500),
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by               BIGINT,
    updated_by               BIGINT,
    UNIQUE (batch_id, student_user_id)
);

-- Individual class sessions (auto-generated from batch schedule)
CREATE TABLE IF NOT EXISTS sports_karate_class (
    id                    BIGSERIAL PRIMARY KEY,
    batch_id              BIGINT      NOT NULL REFERENCES sports_karate_batch(id) ON DELETE CASCADE,
    community_id          BIGINT      NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    conducted_by_user_id  BIGINT      REFERENCES app_user(id) ON DELETE SET NULL,
    scheduled_date        DATE        NOT NULL,
    start_time            TIME,
    end_time              TIME,
    status                VARCHAR(30)  NOT NULL DEFAULT 'SCHEDULED',
    topic                 VARCHAR(200),
    class_notes           VARCHAR(1000),
    cancel_reason         VARCHAR(500),
    reminder_sent         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by            BIGINT,
    updated_by            BIGINT
);

-- Per-student attendance per class session
CREATE TABLE IF NOT EXISTS sports_karate_attendance (
    id             BIGSERIAL PRIMARY KEY,
    class_id       BIGINT      NOT NULL REFERENCES sports_karate_class(id) ON DELETE CASCADE,
    enrollment_id  BIGINT      NOT NULL REFERENCES sports_karate_enrollment(id) ON DELETE CASCADE,
    community_id   BIGINT      NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ABSENT',
    marked_at      TIMESTAMP,
    marked_by      BIGINT      REFERENCES app_user(id) ON DELETE SET NULL,
    notes          VARCHAR(300),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT,
    UNIQUE (class_id, enrollment_id)
);

-- Belt grading exams scheduled for a batch
CREATE TABLE IF NOT EXISTS sports_karate_grading_exam (
    id               BIGSERIAL PRIMARY KEY,
    batch_id         BIGINT      NOT NULL REFERENCES sports_karate_batch(id) ON DELETE CASCADE,
    community_id     BIGINT      NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    target_belt_id   BIGINT      NOT NULL REFERENCES sports_karate_belt(id) ON DELETE RESTRICT,
    venue_id         BIGINT      REFERENCES venue(id) ON DELETE SET NULL,
    scheduled_date   DATE        NOT NULL,
    examiner_name    VARCHAR(100),
    max_candidates   INTEGER,
    status           VARCHAR(30)  NOT NULL DEFAULT 'SCHEDULED',
    reminder_sent    BOOLEAN      NOT NULL DEFAULT FALSE,
    notes            VARCHAR(1000),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by       BIGINT,
    updated_by       BIGINT
);

-- Per-student result for a grading exam
CREATE TABLE IF NOT EXISTS sports_karate_exam_result (
    id             BIGSERIAL PRIMARY KEY,
    exam_id        BIGINT       NOT NULL REFERENCES sports_karate_grading_exam(id) ON DELETE CASCADE,
    enrollment_id  BIGINT       NOT NULL REFERENCES sports_karate_enrollment(id) ON DELETE CASCADE,
    community_id   BIGINT       NOT NULL REFERENCES community(id) ON DELETE CASCADE,
    new_belt_id    BIGINT       REFERENCES sports_karate_belt(id) ON DELETE SET NULL,
    graded_by      BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    passed         BOOLEAN      NOT NULL DEFAULT FALSE,
    score          DECIMAL(5,2),
    remarks        VARCHAR(500),
    graded_at      TIMESTAMP,
    belt_updated   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT,
    UNIQUE (exam_id, enrollment_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_karate_belt_community_rank   ON sports_karate_belt(community_id, rank);
CREATE INDEX IF NOT EXISTS idx_karate_program_community     ON sports_karate_program(community_id, active);
CREATE INDEX IF NOT EXISTS idx_karate_batch_program         ON sports_karate_batch(program_id, status);
CREATE INDEX IF NOT EXISTS idx_karate_batch_community       ON sports_karate_batch(community_id, status);
CREATE INDEX IF NOT EXISTS idx_karate_enrollment_batch      ON sports_karate_enrollment(batch_id, status);
CREATE INDEX IF NOT EXISTS idx_karate_enrollment_student    ON sports_karate_enrollment(student_user_id, status);
CREATE INDEX IF NOT EXISTS idx_karate_enrollment_eligible   ON sports_karate_enrollment(batch_id, grading_eligible) WHERE grading_eligible = TRUE;
CREATE INDEX IF NOT EXISTS idx_karate_class_batch_date      ON sports_karate_class(batch_id, scheduled_date, status);
CREATE INDEX IF NOT EXISTS idx_karate_class_reminder        ON sports_karate_class(scheduled_date, reminder_sent) WHERE status = 'SCHEDULED' AND reminder_sent = FALSE;
CREATE INDEX IF NOT EXISTS idx_karate_attendance_class      ON sports_karate_attendance(class_id);
CREATE INDEX IF NOT EXISTS idx_karate_attendance_enrollment ON sports_karate_attendance(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_karate_exam_batch            ON sports_karate_grading_exam(batch_id, status);
CREATE INDEX IF NOT EXISTS idx_karate_exam_reminder         ON sports_karate_grading_exam(scheduled_date, reminder_sent) WHERE status = 'SCHEDULED' AND reminder_sent = FALSE;
