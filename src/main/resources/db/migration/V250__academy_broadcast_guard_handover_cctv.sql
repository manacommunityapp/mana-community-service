-- Academy, Broadcast, Guard shift handover, CCTV Camera (audit-entity) tables

CREATE TABLE academy_program (
    id              BIGSERIAL     PRIMARY KEY,
    community_id    BIGINT        NOT NULL,
    title           TEXT          NOT NULL,
    description     TEXT,
    category        VARCHAR(50)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    instructor_id   BIGINT,
    instructor_name VARCHAR(100),
    max_seats       INT           NOT NULL DEFAULT 0,
    enrolled_count  INT           NOT NULL DEFAULT 0,
    fee             NUMERIC(10,2) NOT NULL DEFAULT 0,
    start_date      DATE,
    end_date        DATE,
    schedule_text   VARCHAR(200),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_academy_program_community ON academy_program(community_id);

CREATE TABLE academy_enrollment (
    id               BIGSERIAL   PRIMARY KEY,
    program_id       BIGINT      NOT NULL REFERENCES academy_program(id) ON DELETE CASCADE,
    user_id          BIGINT      NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    attendance_count INT         NOT NULL DEFAULT 0,
    enrolled_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (program_id, user_id)
);
CREATE INDEX idx_academy_enrollment_program ON academy_enrollment(program_id);
CREATE INDEX idx_academy_enrollment_user    ON academy_enrollment(user_id);

CREATE TABLE broadcasts (
    id                  BIGSERIAL   PRIMARY KEY,
    community_id        BIGINT,
    created_by_user_id  BIGINT,
    title               TEXT,
    message             TEXT,
    category            VARCHAR(50) DEFAULT 'GENERAL',
    priority            VARCHAR(20) DEFAULT 'NORMAL',
    target              VARCHAR(20) DEFAULT 'ALL',
    status              VARCHAR(20) DEFAULT 'SENT',
    scheduled_at        TIMESTAMP,
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT
);
CREATE INDEX idx_broadcasts_community ON broadcasts(community_id);

CREATE TABLE guard_shift_handover (
    id                  BIGSERIAL   PRIMARY KEY,
    community_id        BIGINT,
    created_by_id       BIGINT,
    acknowledged_by_id  BIGINT,
    shift_date          DATE        NOT NULL,
    shift_type          VARCHAR(20) DEFAULT 'MORNING',
    status              VARCHAR(20) DEFAULT 'PENDING',
    notes               TEXT,
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT
);
CREATE INDEX idx_guard_shift_handover_community ON guard_shift_handover(community_id);
CREATE INDEX idx_guard_shift_handover_date      ON guard_shift_handover(shift_date);

CREATE TABLE guard_handover_item (
    id              BIGSERIAL   PRIMARY KEY,
    handover_id     BIGINT      NOT NULL REFERENCES guard_shift_handover(id) ON DELETE CASCADE,
    category        VARCHAR(50),
    description     TEXT,
    priority        VARCHAR(20) DEFAULT 'MEDIUM',
    resolved_before BOOLEAN     DEFAULT FALSE
);
CREATE INDEX idx_guard_handover_item_handover ON guard_handover_item(handover_id);

-- CCTV Camera (audit-entity variant — distinct from security_camera added in V234)
CREATE TABLE cameras (
    id           BIGSERIAL   PRIMARY KEY,
    community_id BIGINT,
    name         TEXT,
    location     TEXT,
    stream_url   TEXT,
    snapshot_url TEXT,
    status       VARCHAR(20) DEFAULT 'ONLINE',
    is_night_mode BOOLEAN    DEFAULT FALSE,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_by   BIGINT
);
CREATE INDEX idx_cameras_community ON cameras(community_id);
