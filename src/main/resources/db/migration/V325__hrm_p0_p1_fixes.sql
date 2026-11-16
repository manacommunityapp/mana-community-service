-- P0: Unique shift assignment per employee per date
ALTER TABLE hr_employee_shifts
    ADD CONSTRAINT uq_emp_shift_date UNIQUE (employee_id, shift_date);

-- P1: Attendance sessions for multi-punch support
CREATE TABLE hr_attendance_sessions (
    id            BIGSERIAL PRIMARY KEY,
    attendance_id BIGINT NOT NULL,
    session_in    TIMESTAMP NOT NULL,
    session_out   TIMESTAMP,
    break_duration_minutes INT DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_att_session_att FOREIGN KEY (attendance_id) REFERENCES hr_attendance(id) ON DELETE CASCADE
);
CREATE INDEX idx_att_session_att ON hr_attendance_sessions(attendance_id);

-- P1: Extra columns on hr_attendance for night-shift and break tracking
ALTER TABLE hr_attendance
    ADD COLUMN IF NOT EXISTS total_break_minutes INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_night_shift      BOOLEAN DEFAULT FALSE;
