-- Academy sessions: community-led workshops, courses, and tutoring
CREATE TABLE academy_sessions (
  id               BIGSERIAL PRIMARY KEY,
  community_id     BIGINT NOT NULL,
  instructor_id    BIGINT NOT NULL,
  title            VARCHAR(300) NOT NULL,
  description      TEXT NOT NULL,
  category         VARCHAR(100) NOT NULL,
  session_type     VARCHAR(30) NOT NULL DEFAULT 'WORKSHOP',
  format           VARCHAR(20) NOT NULL DEFAULT 'IN_PERSON',
  location         VARCHAR(300),
  online_link      VARCHAR(500),
  starts_at        TIMESTAMPTZ NOT NULL,
  ends_at          TIMESTAMPTZ NOT NULL,
  max_seats        INT,
  enrolled_count   INT NOT NULL DEFAULT 0,
  price            NUMERIC(10,2) NOT NULL DEFAULT 0.00,
  currency         VARCHAR(10) NOT NULL DEFAULT 'INR',
  is_recurring     BOOLEAN NOT NULL DEFAULT FALSE,
  recurrence_rule  VARCHAR(200),
  status           VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  cover_image      VARCHAR(500),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE academy_session_enrollments (
  id               BIGSERIAL PRIMARY KEY,
  session_id       BIGINT NOT NULL REFERENCES academy_sessions(id),
  student_id       BIGINT NOT NULL,
  enrolled_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  payment_status   VARCHAR(30) NOT NULL DEFAULT 'FREE',
  payment_ref      VARCHAR(200),
  attended         BOOLEAN,
  feedback_rating  SMALLINT,
  feedback_text    TEXT,
  UNIQUE(session_id, student_id)
);

CREATE INDEX idx_academy_sessions_community ON academy_sessions(community_id, status, starts_at);
CREATE INDEX idx_academy_sessions_instructor ON academy_sessions(instructor_id);
CREATE INDEX idx_academy_session_enrollments_student ON academy_session_enrollments(student_id);
