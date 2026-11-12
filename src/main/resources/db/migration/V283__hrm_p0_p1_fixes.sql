-- P0: Unique constraint - one check-in per employee per day
-- (attendance table already has unique on employee_id + attendance_date, so the daily record is unique)
-- Adding check-in uniqueness at the log level via application logic is handled in service layer.
-- We add a generated column index for safety on MySQL 5.7+:
-- Note: hr_attendance already has UNIQUE(employee_id, attendance_date) which enforces one record per day.

-- P0: Unique shift assignment per employee per date
ALTER TABLE hr_employee_shifts
    ADD CONSTRAINT uq_emp_shift_date UNIQUE (employee_id, shift_date);

-- P1: Attendance sessions for multi-punch support
CREATE TABLE hr_attendance_sessions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    attendance_id BIGINT NOT NULL,
    session_in  DATETIME NOT NULL,
    session_out DATETIME,
    break_duration_minutes INT DEFAULT 0,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (attendance_id) REFERENCES hr_attendance(id) ON DELETE CASCADE,
    INDEX idx_att_session_att (attendance_id)
);

-- P1: Extra columns on hr_attendance for night-shift and break tracking
ALTER TABLE hr_attendance
    ADD COLUMN IF NOT EXISTS total_break_minutes INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_night_shift      BOOLEAN DEFAULT FALSE;
