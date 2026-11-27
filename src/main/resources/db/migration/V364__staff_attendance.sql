CREATE TYPE attendance_status AS ENUM ('PRESENT','ABSENT','HALF_DAY','ON_LEAVE');
CREATE TYPE leave_status AS ENUM ('PENDING','APPROVED','REJECTED','CANCELLED');
CREATE TYPE leave_type AS ENUM ('SICK','CASUAL','EARNED','UNPAID');

CREATE TABLE staff_shift (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    staff_user_id   BIGINT NOT NULL,
    shift_date      DATE NOT NULL,
    expected_start  TIME NOT NULL,
    expected_end    TIME NOT NULL,
    actual_start    TIMESTAMP,
    actual_end      TIMESTAMP,
    status          attendance_status NOT NULL DEFAULT 'ABSENT',
    marked_by       BIGINT,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(community_id, staff_user_id, shift_date)
);

CREATE TABLE staff_leave_request (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL,
    staff_user_id   BIGINT NOT NULL,
    leave_type      leave_type NOT NULL,
    from_date       DATE NOT NULL,
    to_date         DATE NOT NULL,
    reason          TEXT,
    status          leave_status NOT NULL DEFAULT 'PENDING',
    reviewed_by     BIGINT,
    reviewed_at     TIMESTAMP,
    review_notes    TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_staff_shift_comm ON staff_shift(community_id, shift_date);
CREATE INDEX idx_staff_shift_user ON staff_shift(staff_user_id, shift_date);
CREATE INDEX idx_leave_comm       ON staff_leave_request(community_id, status);
